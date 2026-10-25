package com.heardYou.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConditionalOnBean(DataSourceProperties.class)
    public DataSource dataSource(DataSourceProperties properties) {
        String url = toJdbcUrl(properties.getUrl());
        return properties.initializeDataSourceBuilder()
                .url(url)
                .build();
    }

    private String toJdbcUrl(String url) {
        if (url == null || url.startsWith("jdbc:postgresql:")) {
            return url;
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!("postgres".equalsIgnoreCase(scheme)
                    || "postgresql".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getRawPath() == null
                    || uri.getRawPath().length() < 2) {
                throw new IllegalArgumentException();
            }

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(uri.getHost());
            if (uri.getPort() >= 0) {
                jdbcUrl.append(':').append(uri.getPort());
            }
            jdbcUrl.append(uri.getRawPath());
            if (uri.getRawQuery() != null) {
                jdbcUrl.append('?').append(uri.getRawQuery());
            }
            return jdbcUrl.toString();
        } catch (RuntimeException exception) {
            // Do not include the original URL in the exception; it may contain credentials.
            throw new IllegalArgumentException(
                    "DATABASE_URL must be a JDBC PostgreSQL URL or a PostgreSQL connection URI.");
        }
    }
}
