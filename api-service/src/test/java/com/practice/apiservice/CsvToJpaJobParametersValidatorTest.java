package com.practice.apiservice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import com.practice.apiservice.batch.validation.CsvToJpaJobParametersValidator;

class CsvToJpaJobParametersValidatorTest {

    CsvToJpaJobParametersValidator validator = new CsvToJpaJobParametersValidator(false);

    private JobParameters validParams() {
        return new JobParametersBuilder()
                .addString("processingRequestId", UUID.randomUUID().toString())
                .addString("configId", "csv_to_jpa_v1")
                .addString("storagePath", "/tmp/input.csv")
                .addString("delimiter", ";")      // opcional
                .addLong("chunkSize", 500L)       // opcional
                .toJobParameters();
    }

    @Test
    void valid_parameters_pass() {
        assertDoesNotThrow(() -> validator.validate(validParams()));
    }

    @Test
    void missing_required_key_fails() {
        JobParameters p = new JobParametersBuilder()
                .addString("processingRequestId", UUID.randomUUID().toString())
                .addString("configId", "csv_to_jpa_v1")
                // storagePath is intentionally missing to test validation
                .addString("delimiter", ";")      // opcional
                .addLong("chunkSize", 500L)       // opcional
                .toJobParameters();
        JobParametersInvalidException ex =
                assertThrows(JobParametersInvalidException.class, () -> validator.validate(p));
        assertTrue(ex.getMessage().contains("storagePath is required"));
    }

    @Test
    void invalid_uuid_fails() {
        JobParameters p = new JobParametersBuilder(validParams())
                .addString("processingRequestId", "not-a-uuid")
                .toJobParameters();
        JobParametersInvalidException ex =
                assertThrows(JobParametersInvalidException.class, () -> validator.validate(p));
        assertTrue(ex.getMessage().contains("must be a valid UUID"));
    }

    @Test
    void out_of_range_chunkSize_fails() {
        JobParameters p = new JobParametersBuilder(validParams())
                .addLong("chunkSize", 50L)
                .toJobParameters();
        JobParametersInvalidException ex =
                assertThrows(JobParametersInvalidException.class, () -> validator.validate(p));
        assertTrue(ex.getMessage().contains("chunkSize"));
    }

    @Test
    void invalid_delimiter_fails() {
        JobParameters p = new JobParametersBuilder(validParams())
                .addString("delimiter", "|")
                .toJobParameters();
        JobParametersInvalidException ex =
                assertThrows(JobParametersInvalidException.class, () -> validator.validate(p));
        assertTrue(ex.getMessage().contains("delimiter must be one of"));
    }
}

