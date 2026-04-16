package com.ziyara.backend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures auth tables (password_reset_tokens, otp_verification) exist on startup.
 * Runs migration 009 DDL if tables are missing so the app works without manual psql.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class AuthTablesMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> missing = new ArrayList<>();
            try (Connection c = dataSource.getConnection()) {
                DatabaseMetaData meta = c.getMetaData();
                String schema = "public";
                try (ResultSet rs = meta.getTables(null, schema, "password_reset_tokens", null)) {
                    if (!rs.next()) missing.add("password_reset_tokens");
                }
                try (ResultSet rs = meta.getTables(null, schema, "otp_verification", null)) {
                    if (!rs.next()) missing.add("otp_verification");
                }
            }
            if (missing.isEmpty()) {
                log.debug("Auth tables already exist");
                return;
            }
            log.info("Creating missing auth tables: {}", missing);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            if (missing.contains("password_reset_tokens")) {
                jdbc.execute("CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                        "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
                        "user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                        "token VARCHAR(255) NOT NULL, " +
                        "expires_at TIMESTAMP WITH TIME ZONE NOT NULL, " +
                        "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at)");
            }
            if (missing.contains("otp_verification")) {
                jdbc.execute("CREATE TABLE IF NOT EXISTS otp_verification (" +
                        "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
                        "email_or_phone VARCHAR(255) NOT NULL, " +
                        "otp VARCHAR(10) NOT NULL, " +
                        "expires_at TIMESTAMP WITH TIME ZONE NOT NULL, " +
                        "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_otp_verification_email_or_phone ON otp_verification(email_or_phone)");
                jdbc.execute("CREATE INDEX IF NOT EXISTS idx_otp_verification_expires_at ON otp_verification(expires_at)");
            }
            log.info("Auth tables created successfully");
        } catch (Exception e) {
            log.warn("Could not ensure auth tables exist: {}. Forgot password and OTP will fail until migration 009 is applied.", e.getMessage());
        }
    }
}
