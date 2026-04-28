package com.example.Hackathon.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup migration — ensures MySQL ENUM columns are VARCHAR
 * so new enum values (DEBIT_CARD, NET_BANKING, etc.) can be stored.
 * JPA ddl-auto=update does NOT alter existing ENUM columns.
 */
@Component
public class DatabaseMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            System.out.println("🔧 Running database column migration...");

            // Convert product_type from MySQL ENUM to VARCHAR(50) so new values work
            safeAlter("ALTER TABLE complaints MODIFY COLUMN product_type VARCHAR(50)");
            // Convert issue_type
            safeAlter("ALTER TABLE complaints MODIFY COLUMN issue_type VARCHAR(50)");
            // Convert severity
            safeAlter("ALTER TABLE complaints MODIFY COLUMN severity VARCHAR(20)");
            // Convert status
            safeAlter("ALTER TABLE complaints MODIFY COLUMN status VARCHAR(30)");
            // Convert channel
            safeAlter("ALTER TABLE complaints MODIFY COLUMN channel VARCHAR(30)");

            System.out.println("✅ Database column migration complete.");
        } catch (Exception e) {
            System.out.println("⚠️ Database migration skipped or failed: " + e.getMessage());
        }
    }

    private void safeAlter(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Column may already be VARCHAR or table may not exist yet
            System.out.println("  ↳ Skipped: " + e.getMessage());
        }
    }
}
