package com.farmmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// removed unused import
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DbSeedingService {

    private static final Logger log = LoggerFactory.getLogger(DbSeedingService.class);
    private final JdbcTemplate jdbcTemplate;

    public DbSeedingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Seeds the database with initial data if it's empty.
     */
    public void seedDatabase() {
        log.info("Seeding database with initial data...");
        try {
            // Check if tables are already populated (simple check on a known table)
            // This prevents re-seeding on every application restart if the DB exists.
            if (isDatabaseSeeded()) {
                log.info("Database already seeded. Skipping seeding process.");
                return;
            }

            executeSqlScript("database/seed.sql");
            log.info("Database seeding complete.");
        } catch (IOException e) {
            log.error("Failed to read seed SQL script", e);
            throw new RuntimeException("Failed to seed database", e);
        } catch (SQLException e) {
            log.error("Failed to execute seed SQL script", e);
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    private boolean isDatabaseSeeded() throws SQLException {
        // A simple check: if there's at least one record in 'fields' table, assume it's seeded.
        // This is a pragmatic approach; a more robust check might involve a dedicated seed status table.
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fields", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            // If table doesn't exist or other SQL error, assume not seeded.
            log.warn("Could not check seed status (table 'fields' might not exist yet): {}", e.getMessage());
            return false;
        }
    }

    private void executeSqlScript(String scriptPath) throws IOException, SQLException {
        log.info("Executing SQL seed script: {}", scriptPath);
        try (InputStream is = new ClassPathResource(scriptPath).getInputStream()) {
            String scriptContent = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Simple splitting by semicolon. For more complex scripts, a dedicated SQL parser would be better.
            String[] statements = scriptContent.split(";\s*");
            Connection conn = jdbcTemplate.getDataSource().getConnection();

            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    try {
                        conn.createStatement().execute(statement);
                        log.debug("Executed statement: {}", statement);
                    } catch (SQLException e) {
                        // Log and continue, some statements might be for existing data or fail benignly
                        log.warn("Could not execute statement in seed script ({}): {}", statement, e.getMessage());
                    }
                }
            }
            log.info("Finished executing SQL seed script: {}", scriptPath);
        }
    }
}
