package com.practice.apiservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Sql("/org/springframework/batch/core/schema-h2.sql")
class BatchInfraSmokeTest {

    @Autowired JobRegistry jobRegistry;
    @Autowired JobLauncher jobLauncher;

    @Test
    void canLaunchDemoJob() throws Exception {
        Job job = jobRegistry.getJob("demoJob");
        JobParameters params = new JobParametersBuilder()
                .addString("processingRequestId", java.util.UUID.randomUUID().toString())
                .addString("configId", "csv_to_jpa_v1")
                .addString("storagePath", "/tmp/test-input.csv")
                .addLong("ts", System.currentTimeMillis()) // evita reejecución por parámetros repetidos
                .toJobParameters();

        JobExecution exec = jobLauncher.run(job, params);
        assertThat(exec.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());
    }
}
