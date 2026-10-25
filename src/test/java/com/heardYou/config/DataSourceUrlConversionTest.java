package com.heardYou.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DataSourceConfig.parseUrl().
 *
 * These tests cover every URL scheme the application can receive:
 *   - postgres://   (standard Render / Heroku format)
 *   - postgresql:// (Render alternate format, reported in production failure)
 *   - jdbc:postgresql://  (already-JDBC, pass-through)
 *
 * Tests also verify credential extraction and query-parameter preservation.
 * No Spring context is loaded — the method is package-private and tested directly.
 */
class DataSourceUrlConversionTest {

    // -----------------------------------------------------------------------
    // postgresql:// scheme (the scheme seen in the production failure)
    // -----------------------------------------------------------------------

    @Test
    void postgresql_noCredentials_noPort_convertedCorrectly() {
        var result = DataSourceConfig.parseUrl("postgresql://db.render.com/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com/heardyou");
        assertThat(result.userInfo).isNull();
    }

    @Test
    void postgresql_withPort_convertedCorrectly() {
        var result = DataSourceConfig.parseUrl("postgresql://db.render.com:5432/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com:5432/heardyou");
        assertThat(result.userInfo).isNull();
    }

    @Test
    void postgresql_withEmbeddedCredentials_credentialsExtracted_urlStripped() {
        var result = DataSourceConfig.parseUrl("postgresql://alice:s3cret@db.render.com:5432/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com:5432/heardyou");
        assertThat(result.userInfo).isEqualTo("alice:s3cret");
    }

    @Test
    void postgresql_withQueryParameters_preservedInJdbcUrl() {
        var result = DataSourceConfig.parseUrl("postgresql://db.render.com/heardyou?sslmode=require");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com/heardyou?sslmode=require");
    }

    @Test
    void postgresql_withCredentialsAndQueryParameters_bothHandled() {
        var result = DataSourceConfig.parseUrl(
                "postgresql://alice:s3cret@db.render.com:5432/heardyou?sslmode=require");

        assertThat(result.jdbcUrl)
                .isEqualTo("jdbc:postgresql://db.render.com:5432/heardyou?sslmode=require");
        assertThat(result.userInfo).isEqualTo("alice:s3cret");
    }

    // -----------------------------------------------------------------------
    // postgres:// scheme (Heroku / standard Render legacy format)
    // -----------------------------------------------------------------------

    @Test
    void postgres_noCredentials_noPort_convertedCorrectly() {
        var result = DataSourceConfig.parseUrl("postgres://db.render.com/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com/heardyou");
        assertThat(result.userInfo).isNull();
    }

    @Test
    void postgres_withPort_convertedCorrectly() {
        var result = DataSourceConfig.parseUrl("postgres://db.render.com:5432/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com:5432/heardyou");
    }

    @Test
    void postgres_withEmbeddedCredentials_credentialsExtracted_urlStripped() {
        var result = DataSourceConfig.parseUrl("postgres://alice:s3cret@db.render.com:5432/heardyou");

        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com:5432/heardyou");
        assertThat(result.userInfo).isEqualTo("alice:s3cret");
    }

    // -----------------------------------------------------------------------
    // jdbc:postgresql:// — already JDBC, must pass through unchanged
    // -----------------------------------------------------------------------

    @Test
    void jdbcUrl_passedThrough_unchanged() {
        String input = "jdbc:postgresql://localhost:5432/heardYou";
        var result = DataSourceConfig.parseUrl(input);

        assertThat(result.jdbcUrl).isEqualTo(input);
        assertThat(result.userInfo).isNull();
    }

    @Test
    void jdbcUrl_withQueryParams_passedThrough_unchanged() {
        String input = "jdbc:postgresql://localhost:5432/heardYou?sslmode=require";
        var result = DataSourceConfig.parseUrl(input);

        assertThat(result.jdbcUrl).isEqualTo(input);
    }

    // -----------------------------------------------------------------------
    // Case-insensitive scheme matching
    // -----------------------------------------------------------------------

    @Test
    void postgresUppercase_scheme_accepted() {
        var result = DataSourceConfig.parseUrl("POSTGRES://db.render.com/heardyou");
        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com/heardyou");
    }

    @Test
    void postgresqlUppercase_scheme_accepted() {
        var result = DataSourceConfig.parseUrl("POSTGRESQL://db.render.com/heardyou");
        assertThat(result.jdbcUrl).isEqualTo("jdbc:postgresql://db.render.com/heardyou");
    }

    // -----------------------------------------------------------------------
    // Error cases — must throw IllegalArgumentException, never expose credentials
    // -----------------------------------------------------------------------

    @Test
    void nullUrl_throwsIllegalArgument() {
        assertThatThrownBy(() -> DataSourceConfig.parseUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void unknownScheme_throwsIllegalArgument() {
        assertThatThrownBy(() -> DataSourceConfig.parseUrl("mysql://host/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    void missingDatabase_throwsIllegalArgument() {
        assertThatThrownBy(() -> DataSourceConfig.parseUrl("postgresql://db.render.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database name");
    }

    @Test
    void errorMessage_doesNotContainCredentials() {
        // Passing a bad URL should throw, and the message must not echo the raw URL.
        assertThatThrownBy(() -> DataSourceConfig.parseUrl("not-a-url-at-all"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("not-a-url-at-all");
    }
}
