package com.example.demo.device.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class FlywayConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (Throwable ex) {
                var cause = ex;
                while (cause != null) {
                    if (cause instanceof SQLException exception && ("40001".equals(exception.getSQLState()))) {
                        // Flyway is designed to use at least two connections, one for the
                        // metadata table and one for the migrations. If the migration
                        // flow modifies the system catalog, queries in the metadata
                        // session will fail with the catalog snapshot exception because
                        // flyway keeps the same transaction for the metadata connection.
                        // It needs to be retried.
                        LOGGER.warn("Retrying Flyway migration due to 40001 exception: ", cause);
                        flyway.migrate();
                        break;
                    }

                    cause = cause.getCause();
                }
            }
        };
    }
}
