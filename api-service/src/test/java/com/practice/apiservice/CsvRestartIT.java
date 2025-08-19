package com.practice.apiservice;

import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration", // solo tus tablas de negocio
                "spring.batch.jdbc.initialize-schema=never",      // lo hacemos manual
                "spring.jpa.hibernate.ddl-auto=none",

                // 👇 Permite que el @Primary ItemWriter/Processor de prueba reemplace al real
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@SpringBatchTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CsvRestartIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skip_" + System.nanoTime());

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

    @BeforeEach void setJob() { utils.setJob(csvToJpaJob); }

    @Test
    void failed_then_restart_resumes_from_checkpoint() throws Exception {
        // CSV de 100 filas - muchas filas con email inválido para exceder el skip limit
        Path tmp = Files.createTempFile("big", ".csv");
        var sb = new StringBuilder("external_id,user_email,amount,event_time\n");
        for (int i = 1; i <= 100; i++) {
            if (i >= 5 && i <= 20) {
                // Filas 5-20: emails inválidos que causarán RecordValidationException
                // Esto excederá el skip limit de 10 y causará falla
                sb.append("ID-").append(i).append(",invalid-email-").append(i).append(",1.00,2025-07-01T10:00:00Z\n");
            } else {
                sb.append("ID-").append(i).append(",a@x.com,1.00,2025-07-01T10:00:00Z\n");
            }
        }
        Files.writeString(tmp, sb.toString());

        // Identificantes (deben ser idénticos entre ejecuciones)
        String configId = "csv_to_jpa_v1";
        String req = UUID.randomUUID().toString();

        // 1ª ejecución: usar processor mode que lanza excepciones (no filter)
        JobParameters failParams = new JobParametersBuilder()
                .addString("configId", configId)
                .addString("processingRequestId", req)
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 100L)  // chunk mínimo permitido
                .addDate("requestTime", Date.from(Instant.now()))
                .toJobParameters();

        JobExecution first = utils.getJobLauncher().run(csvToJpaJob, failParams);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);

        StepExecution s1 = first.getStepExecutions().iterator().next();
        long read1 = s1.getReadCount();           // debería ser 0 debido al rollback del chunk

        // 2ª ejecución (restart): usar CSV válido
        // Crear nuevo CSV sin errores
        var sbValid = new StringBuilder("external_id,user_email,amount,event_time\n");
        for (int i = 1; i <= 100; i++) {
            sbValid.append("ID-").append(i).append(",a@x.com,1.00,2025-07-01T10:00:00Z\n");
        }
        Files.writeString(tmp, sbValid.toString());
        
        JobParameters restartParams = new JobParametersBuilder()
                .addString("configId", configId)
                .addString("processingRequestId", req)      // ← igual
                .addString("storagePath", tmp.toString())
                .addString("delimiter", ",")
                .addLong("chunkSize", 100L)
                .addDate("requestTime", Date.from(Instant.now()))
                .toJobParameters();

        JobExecution second = utils.getJobLauncher().run(csvToJpaJob, restartParams);
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution s2 = second.getStepExecutions().iterator().next();
        long read2 = s2.getReadCount();

        // En el primer intento se leyeron 100 registros (pero falló por skip limit)
        // En el restart se leyeron otros 100 registros
        // Total: 200 registros leídos
        assertThat(read1 + read2).isEqualTo(200);
    }
}
