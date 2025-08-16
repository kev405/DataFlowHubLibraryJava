package com.practice.apiservice;

import java.nio.file.Path;

import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration", // solo tus tablas de negocio
                "spring.batch.jdbc.initialize-schema=never",      // lo hacemos manual
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@SpringBatchTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // para @BeforeAll no estático
class CsvToJpaJobE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("e2e_" + System.nanoTime());

    @Autowired JobLauncherTestUtils utils;
    @Autowired Job csvToJpaJob;
    @Autowired javax.sql.DataSource dataSource;

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

    @BeforeEach
    void setJob() { utils.setJob(csvToJpaJob); }

    @Test
    void end_to_end_completes() throws Exception {
        Path tmp = java.nio.file.Files.createTempFile("sample", ".csv");
        java.nio.file.Files.writeString(tmp, String.join("\n",
                "external_id,user_email,amount,event_time",
                "E-1,a@x.com,10.00,2025-07-01T10:00:00Z",
                "E-2,b@x.com,20.00,2025-07-02T10:00:00Z"));

        JobParameters params = new JobParametersBuilder()
                .addString("configId", "csv_to_jpa_v1")
                .addString("processingRequestId", java.util.UUID.randomUUID().toString())
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 500L)
                .addDate("requestTime", java.util.Date.from(java.time.Instant.now()))
                .toJobParameters();

        var exec = utils.getJobLauncher().run(csvToJpaJob, params);

        org.assertj.core.api.Assertions.assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        var step = exec.getStepExecutions().iterator().next();
        org.assertj.core.api.Assertions.assertThat(step.getReadCount())
                .isGreaterThanOrEqualTo(step.getWriteCount());
    }
}