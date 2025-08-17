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

                // 👇 Permite que el @Primary ItemWriter de prueba reemplace al real
                "spring.main.allow-bean-definition-overriding=true",

                // Retry de ejemplo
                "batch.csv.retry.limit=3",
                "batch.csv.retry.initial=100ms",
                "batch.csv.retry.multiplier=2.0",
                "batch.csv.retry.max=1s"
        }
)
@SpringBatchTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryPolicyIT {

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
    static class FailingWriterConfig {
        private int attempts = 0;

        /** Falla 2 veces; a la 3ª "escribe" OK (no-op). */
        @Bean @Primary
        @StepScope
        JdbcBatchItemWriter<ImportRecord> importRecordWriter() {
            return new JdbcBatchItemWriter<ImportRecord>() {
                @Override
                public void write(Chunk<? extends ImportRecord> items) throws Exception {
                    attempts++;
                    if (attempts <= 2) {
                        throw new ConcurrencyFailureException("simulated transient DB error");
                    }
                    // En el 3er intento, no hacer nada (éxito)
                }

                @Override
                public void afterPropertiesSet() {
                    // No hacer nada para evitar la validación de configuración
                }
            };
        }
    }

    @Test
    void writer_retries_and_completes_on_third_attempt() throws Exception {
        Path tmp = Files.createTempFile("retry-ok", ".csv");
        Files.writeString(tmp, String.join("\n",
                "external_id,user_email,amount,event_time",
                "R-1,a@x.com,10.00,2025-07-01T10:00:00Z"));

        JobParameters p = new JobParametersBuilder()
                .addString("configId", "csv_to_jpa_v1")
                .addString("processingRequestId", UUID.randomUUID().toString())
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 100L)
                .addDate("requestTime", Date.from(Instant.now()))
                .toJobParameters();

        JobExecution exec = utils.getJobLauncher().run(csvToJpaJob, p);
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
