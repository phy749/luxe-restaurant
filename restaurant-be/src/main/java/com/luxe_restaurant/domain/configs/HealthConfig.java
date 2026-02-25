package com.luxe_restaurant.domain.configs;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class HealthConfig {

    @Component
    public static class DatabaseHealthIndicator implements HealthIndicator {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Override
        public Health health() {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return Health.up()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "Connected")
                        .withDetail("timestamp", LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "Disconnected")
                        .withDetail("error", e.getMessage())
                        .withDetail("timestamp", LocalDateTime.now())
                        .build();
            }
        }
    }

    @Component
    public static class ApplicationInfoContributor implements InfoContributor {

        @Override
        public void contribute(Info.Builder builder) {
            Map<String, Object> details = new HashMap<>();
            details.put("name", "Luxe Restaurant Management System");
            details.put("version", "1.0.0");
            details.put("description", "A modern restaurant management system with AI chatbot");
            details.put("startup-time", LocalDateTime.now());
            details.put("java-version", System.getProperty("java.version"));
            details.put("spring-boot-version", "3.5.6");
            
            builder.withDetails(details);
        }
    }

    @Component
    public static class CustomHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            // Check application-specific health metrics
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

            Health.Builder healthBuilder = memoryUsagePercent < 90 ? Health.up() : Health.down();

            return healthBuilder
                    .withDetail("memory-usage-percent", String.format("%.2f%%", memoryUsagePercent))
                    .withDetail("free-memory-mb", freeMemory / 1024 / 1024)
                    .withDetail("total-memory-mb", totalMemory / 1024 / 1024)
                    .withDetail("used-memory-mb", usedMemory / 1024 / 1024)
                    .build();
        }
    }
}