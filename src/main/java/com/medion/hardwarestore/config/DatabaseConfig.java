package com.medion.hardwarestore.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${DB_URL:jdbc:postgresql://localhost:5432/hardware_db}")
    private String dbUrl;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:password}")
    private String dbPassword;

    @Bean
    public DataSource dataSource() {
        if (dbUrl != null && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            String cleanUrl = dbUrl.replaceFirst("^postgres(?:ql)?://", "");
            String[] parts = cleanUrl.split("@");
            if (parts.length == 2) {
                String[] credentials = parts[0].split(":");
                dbUser = credentials[0];
                if (credentials.length > 1) {
                    dbPassword = credentials[1];
                }
                dbUrl = "jdbc:postgresql://" + parts[1];
            } else {
                dbUrl = "jdbc:postgresql://" + cleanUrl;
            }
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }
}
