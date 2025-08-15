package com.practice.apiservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@ContextConfiguration(classes = {
        ReexecutionIncrementerTest.TestConfig.class,
        // Importa SOLO lo necesario para el job e incrementer
        com.practice.apiservice.config.BatchConfig.class,                 // tu DefaultBatchConfiguration
        com.practice.apiservice.batch.DemoJobConfig.class,                // job/step/validator (usa listeners)
        com.practice.apiservice.batch.incrementer.IncrementerConfig.class // runId / requestTime
})
@TestPropertySource(properties = {
        "spring.batch.job.enabled=false",
        "spring.batch.jdbc.initialize-schema=always",
        "batch.incrementer=runId"
})
class ReexecutionIncrementerTest {

    @Autowired private JobParametersIncrementer incrementer;
    @Configuration
    @ComponentScan(basePackages = "com.practice.apiservice.batch.listener")
    static class TestConfig {
        /** H2 embebida con esquema de Spring Batch cargado */
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("batchtest")
                    .addScript("org/springframework/batch/core/schema-h2.sql")
                    .build();
        }

        /** TX manager que usa el DS embebido */
        @Bean
        PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        /** MeterRegistry simple para los listeners */
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        /** Utilidad de pruebas Batch */
        @Bean
        JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils();
        }
    }

    @Autowired private JobLauncherTestUtils utils;

    @Autowired
    @Qualifier("demoJob")
    private Job demoJob;

    @BeforeEach
    void setup() { utils.setJob(demoJob); }

    @Test
    void launching_twice_with_incrementer_creates_distinct_jobInstances() throws Exception {
        JobParameters base = new JobParametersBuilder()
                .addString("processingRequestId", UUID.randomUUID().toString())
                .addString("configId", "csv_to_jpa_v1")
                .addString("storagePath", "/tmp/input.csv")
                .toJobParameters();

        // 1ª ejecución: parámetros base
        JobExecution first = utils.launchJob(base);

        // 2ª ejecución: aplica el incrementer (run.id o requestTime)
        JobParameters next = incrementer.getNext(base);

        JobExecution second = utils.launchJob(next);

        assertThat(first.getJobInstance().getInstanceId())
                .isNotEqualTo(second.getJobInstance().getInstanceId());
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
