package com.practice.apiservice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.ConcurrencyFailureException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.practice.apiservice.model.ImportRecord;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration", // solo tus tablas de negocio
                "spring.batch.jdbc.initialize-schema=never",      // lo hacemos manual
                "spring.jpa.hibernate.ddl-auto=none",
                // 👇 Permite override del writer
                "spring.main.allow-bean-definition-overriding=true",

                // Límite bajo para forzar agotamiento
                "batch.csv.retry.limit=2",
                "batch.csv.retry.initial=50ms",
                "batch.csv.retry.multiplier=2.0",
                "batch.csv.retry.max=500ms"
        }
)
@SpringBatchTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryPolicyExhaustedIT {

    @Autowired JobLauncherTestUtils utils;
    @Autowired Job csvToJpaJob;
    @Autowired javax.sql.DataSource dataSource;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skip_" + System.nanoTime());

    @BeforeAll
    void initBatchSchema() {
        // Drop + Create de las tablas BATCH_* correctas para PostgreSQL
        var pop = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
        pop.setContinueOnError(true);
        pop.setIgnoreFailedDrops(true);
        pop.addScript(new org.springframework.core.io.ClassPathResource(
                "org/springframework/batch/core/schema-drop-postgresql.sql"));
        pop.addScript(new org.springframework.core.io.ClassPathResource(
                "org/springframework/batch/core/schema-postgresql.sql"));
        org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute(pop, dataSource);
    }

    @BeforeEach void setJob() { utils.setJob(csvToJpaJob); }

    @TestConfiguration
    static class AlwaysFailWriterConfig {
        /** Siempre falla con excepción transitoria (no deprecada). */
        @Bean @Primary
        @StepScope
        JdbcBatchItemWriter<ImportRecord> importRecordWriter() {
            // Crear un simple mock que implemente la interfaz pero siempre falle
            return new JdbcBatchItemWriter<ImportRecord>() {
                @Override
                public void write(Chunk<? extends ImportRecord> items) throws Exception {
                    throw new ConcurrencyFailureException("always failing for retry test");
                }

                @Override
                public void afterPropertiesSet() {
                    // No hacer nada para evitar la validación de configuración
                }
            };
        }
    }

    @Test
    void writer_exhausts_retry_limit_and_fails_step() throws Exception {
        Path tmp = Files.createTempFile("retry-fail", ".csv");
        Files.writeString(tmp, String.join("\n",
                "external_id,user_email,amount,event_time",
                "R-2,a@x.com,10.00,2025-07-01T10:00:00Z"));

        JobParameters p = new JobParametersBuilder()
                .addString("configId", "csv_to_jpa_v1")
                .addString("processingRequestId", UUID.randomUUID().toString())
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 100L)
                .addDate("requestTime", Date.from(Instant.now()))
                .toJobParameters();

        JobExecution exec = utils.getJobLauncher().run(csvToJpaJob, p);
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
    }
}
