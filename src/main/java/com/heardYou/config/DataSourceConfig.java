package com.heardYou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Constructs the primary DataSource from environment variables.
 *
 * <p>Render (and similar platforms) provide the database connection string as a
 * PostgreSQL URI ({@code postgres://user:pass@host:port/db} or
 * {@code postgresql://user:pass@host:port/db}). Spring Boot's JDBC layer requires a
 * JDBC URL ({@code jdbc:postgresql://host:port/db}). This class converts the URI to
 * JDBC format at startup.
 *
 * <p>Credentials are resolved in this priority order:
 * <ol>
 *   <li>If {@code DB_USER} / {@code DB_PASSWORD} environment variables are explicitly set,
 *       those values are used regardless of what appears in the URI.</li>
 *   <li>If only the URI contains embedded user-info (e.g.
 *       {@code postgresql://alice:secret@host/db}), those are extracted and used.</li>
 *   <li>The username defaults to {@code postgres} if nothing else is configured.</li>
 *   <li>The password has no default — an unconfigured password is passed as {@code null}
 *       and the driver treats it as no-password.</li>
 * </ol>
 *
 * <p>{@code @ConditionalOnMissingBean(DataSource.class)} ensures this bean is skipped
 * when a test provides a mock {@code DataSource}, while always activating in production
 * where no other {@code DataSource} is registered.
 */
@Configuration
public class DataSourceConfig {

    // Raw DATABASE_URL value — may be a jdbc: URL or a postgres(ql):// URI.
    // The local default keeps development working without any environment variables.
    @Value("${DATABASE_URL:jdbc:postgresql://localhost:5432/heardYou}")
    private String rawDatabaseUrl;

    // Explicit overrides. DB_USER defaults to "postgres" for local dev.
    // DB_PASSWORD has NO default — it will be null when the env var is absent.
    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:#{null}}")
    private String dbPassword;

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        ParsedUrl parsed = parseUrl(rawDatabaseUrl);

        // Explicit env vars take priority over anything embedded in the URL.
        String username = dbUser;
        String password = dbPassword;

        // If DB_USER was not overridden from the default and the URL contains
        // embedded credentials, use them as the fallback.
        if ("postgres".equals(username) && parsed.userInfo != null && !parsed.userInfo.isEmpty()) {
            int colon = parsed.userInfo.indexOf(':');
            if (colon > 0) {
                username = parsed.userInfo.substring(0, colon);
                if (password == null) {
                    password = parsed.userInfo.substring(colon + 1);
                }
            } else {
                username = parsed.userInfo;
            }
        }

        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(parsed.jdbcUrl)
                .driverClassName("org.postgresql.Driver")
                .username(username);
        if (password != null) {
            builder = builder.password(password);
        }
        return builder.build();
    }

    // Package-private for testing.
    static ParsedUrl parseUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException(
                    "DATABASE_URL must not be null.");
        }
        if (url.startsWith("jdbc:postgresql:")) {
            // Already a valid JDBC URL — pass through unchanged.
            return new ParsedUrl(url, null);
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!("postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(
                        "DATABASE_URL scheme must be postgres, postgresql, or jdbc:postgresql.");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException(
                        "DATABASE_URL must contain a hostname.");
            }
            String rawPath = uri.getRawPath();
            if (rawPath == null || rawPath.length() < 2) {
                throw new IllegalArgumentException(
                        "DATABASE_URL must contain a database name (e.g. /mydb).");
            }

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(uri.getHost());
            if (uri.getPort() >= 0) {
                jdbcUrl.append(':').append(uri.getPort());
            }
            jdbcUrl.append(rawPath);
            if (uri.getRawQuery() != null) {
                jdbcUrl.append('?').append(uri.getRawQuery());
            }

            return new ParsedUrl(jdbcUrl.toString(), uri.getUserInfo());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            // Do not include the original URL in the message — it may contain credentials.
            throw new IllegalArgumentException(
                    "DATABASE_URL is not a valid PostgreSQL JDBC URL or connection URI.");
        }
    }

    /** Holds the converted JDBC URL and any user-info extracted from the original URI. */
    static final class ParsedUrl {
        final String jdbcUrl;
        /** Raw user-info string from the URI (e.g. {@code "alice:secret"}), or {@code null}. */
        final String userInfo;

        ParsedUrl(String jdbcUrl, String userInfo) {
            this.jdbcUrl = jdbcUrl;
            this.userInfo = userInfo;
        }
    }
}
