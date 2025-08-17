package com.practice.apiservice.batch;

import com.practice.apiservice.batch.listener.MetricsStepListener;
import com.practice.apiservice.batch.processor.ImportRecordProcessor;
import com.practice.apiservice.batch.processor.RecordValidationException;
import com.practice.apiservice.batch.retry.RetryMetricsListener;
import com.practice.apiservice.batch.retry.RetryProperties;
import com.practice.apiservice.batch.skip.ImportSkipListener;
import com.practice.apiservice.model.ImportRecord;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CsvImportStepConfig {

    @Bean
    @JobScope
    public Step csvImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager tx,
            FlatFileItemReader<ImportRecord> importRecordReader,   // F3-07
            ImportRecordProcessor importRecordProcessor,            // F3-08
            JdbcBatchItemWriter<ImportRecord> importRecordWriter,   // F3-09
            MetricsStepListener stepListener,                       // C0-F3-03
            @Value("#{jobParameters['chunkSize'] ?: 500}") Integer chunkSize,
            ImportSkipListener skipListener,
            @Value("${batch.csv.skip-limit:1000}") int skipLimit,
            RetryProperties retryProps,
            ExponentialBackOffPolicy csvRetryBackoff,
            RetryMetricsListener retryListener
    ) {
        return new StepBuilder("csvImportStep", jobRepository)
                .<ImportRecord, ImportRecord>chunk(chunkSize, tx)
                .reader(importRecordReader)
                .processor(importRecordProcessor)
                .writer(importRecordWriter)
                .listener(stepListener)
                .listener((StepExecutionListener) importRecordProcessor)  // Explicit casting
                .listener((ChunkListener) importRecordProcessor)          // Explicit casting
                .faultTolerant()
                .skip(RecordValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(skipLimit)
                .retry(org.springframework.dao.TransientDataAccessException.class)
                .retry(java.sql.SQLTransientConnectionException.class)
                .retry(java.net.SocketTimeoutException.class)
                .retry(CannotGetJdbcConnectionException.class)
                .retryLimit(retryProps.getLimit())
                .backOffPolicy(csvRetryBackoff)
                .listener(retryListener)
                .listener(skipListener)
                // NOTE: faultTolerant() lo activaremos en C3 (skip/retry).
                .build();
    }
}
