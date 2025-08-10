package com.practice.apiservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.flywaydb.core.Flyway;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class DbSmokeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired DataSource dataSource;
    @Autowired(required = false) Flyway flyway;

    @Test
    void contextLoads_andDatabaseIsReachable() throws Exception {
        try (var conn = dataSource.getConnection()) {
            assertThat(conn.isValid(2)).isTrue();
        }
        // Si todavía no tienes scripts, flyway puede ser null y no pasa nada.
        if (flyway != null) {
            assertThat(flyway.info()).isNotNull();
        }
    }
}
