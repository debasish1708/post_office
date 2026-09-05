package com.postoffice.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs AFTER Hibernate ddl-auto=update (@Order 100) to perform
 * post-migration cleanup and data fixups on an already-created schema.
 */
@Component
@Order(100)
public class SchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public SchemaMigrator(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Drop leftover legacy tables from old schema versions (safe — IF EXISTS)
        jdbcTemplate.execute("DROP TABLE IF EXISTS letters CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS post_types CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS route_nodes CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS transactions CASCADE");

        // Only run data migrations if the users table already exists
        if (!tableExists("users")) {
            return;
        }

        // Migrate old wallet_balance column to balance if both exist
        if (columnExists("users", "wallet_balance") && columnExists("users", "balance")) {
            jdbcTemplate.execute(
                    "UPDATE users SET balance = COALESCE(balance, wallet_balance, 100.00) WHERE balance IS NULL");
        }

        // Ensure all existing users have a hashed password (for pre-existing plain-text rows)
        if (columnExists("users", "password")) {
            String hash = passwordEncoder.encode("ChangeMe123");
            jdbcTemplate.update(
                    "UPDATE users SET password = ? WHERE password IS NULL OR password = ''", hash);
        }

        // Backfill any NULLs in timestamp columns that Hibernate cannot add as NOT NULL otherwise
        backfillTimestamps("users");
        backfillTimestamps("service");
        backfillTimestamps("post_office");
    }

    /**
     * Fills in NULL created_at / updated_at on existing rows so Hibernate's
     * ddl-auto=update can enforce the NOT NULL constraint without failing.
     * This runs AFTER Hibernate has added the columns (which it adds as nullable
     * when rows already exist and the column definition has a DEFAULT).
     */
    private void backfillTimestamps(String table) {
        if (!tableExists(table)) return;
        if (columnExists(table, "created_at")) {
            jdbcTemplate.update("UPDATE " + table + " SET created_at = NOW() WHERE created_at IS NULL");
        }
        if (columnExists(table, "updated_at")) {
            jdbcTemplate.update("UPDATE " + table + " SET updated_at = NOW() WHERE updated_at IS NULL");
        }
        if ("post_office".equals(table) && columnExists(table, "post_date")) {
            jdbcTemplate.update("UPDATE " + table + " SET post_date = NOW() WHERE post_date IS NULL");
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }
}

