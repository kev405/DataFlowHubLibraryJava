package com.practice.apiservice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvSkipIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skip_" + System.nanoTime());

    @Autowired JobLauncherTestUtils utils;
    @Autowired Job csvToJpaJob;
    @Autowired NamedParameterJdbcTemplate jdbc;
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

    @BeforeEach void setJob(){ utils.setJob(csvToJpaJob); }

    @Test
    void step_skips_invalid_rows_and_persists_errors() throws Exception {
        Path tmp = Files.createTempFile("bad-rows", ".csv");
        Files.writeString(tmp, String.join("\n",
                "external_id,user_email,amount,event_time",
                "OK-1,ok@x.com,10.00,2025-07-01T10:00:00Z",
                "BAD-1,bad-email,5.00,2025-07-01T11:00:00Z",
                "OK-2,ok2@x.com,20.00,2025-07-01T12:00:00Z"));

        var req = UUID.randomUUID();
        JobParameters p = new JobParametersBuilder()
                .addString("configId", "csv_to_jpa_v1")
                .addString("processingRequestId", req.toString())
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 500L)
                .addDate("requestTime", Date.from(Instant.now()))
                .toJobParameters();

        var exec = utils.getJobLauncher().run(csvToJpaJob, p);
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        var step = exec.getStepExecutions().iterator().next();
        System.out.println("ReadCount: " + step.getReadCount());
        System.out.println("WriteCount: " + step.getWriteCount());
        System.out.println("SkipCount: " + step.getSkipCount());
        System.out.println("FilterCount: " + step.getFilterCount());
        System.out.println("CommitCount: " + step.getCommitCount());

        // Debug: Check what's in the import_records table
        var importRecordsCount = jdbc.getJdbcTemplate().queryForObject(
                "select count(*) from import_records", Integer.class);
        System.out.println("Total import_records: " + importRecordsCount);

        // Debug: Check what's in the import_errors table
        var errorRecordsCount = jdbc.getJdbcTemplate().queryForObject(
                "select count(*) from import_errors where processing_request_id = ?",
                Integer.class, req);
        System.out.println("Error records for this request: " + errorRecordsCount);

        assertThat(step.getReadCount()).isEqualTo(3);
        assertThat(step.getWriteCount()).isEqualTo(2);
        assertThat(step.getSkipCount()).isEqualTo(1);

        var cnt = jdbc.getJdbcTemplate().queryForObject(
                "select count(*) from import_errors where processing_request_id = ?",
                Integer.class, req);
        assertThat(cnt).isEqualTo(1);
    }
}
