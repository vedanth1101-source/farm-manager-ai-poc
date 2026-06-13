package com.farmmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;
    private final DbSeedingService dbSeedingService;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, DbSeedingService dbSeedingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbSeedingService = dbSeedingService;
    }

    @PostConstruct
    public void initializeDatabase() {
        log.info("Initializing database...");
        try {
            executeSqlScript("database/schema.sql");
            dbSeedingService.seedDatabase(); // Call seeding service after schema is ready
            log.info("Database initialization complete.");
        } catch (IOException | SQLException e) {
            log.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void executeSqlScript(String scriptPath) throws IOException, SQLException {
        log.info("Executing SQL script: {}", scriptPath);
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
                        // Log and continue, some statements might be for existing tables etc.
                        log.warn("Could not execute statement ({}): {}", statement, e.getMessage());
                    }
                }
            }
            log.info("Finished executing SQL script: {}", scriptPath);
        }
    }
}
