package com.practice.apiservice.batch;

import com.practice.apiservice.batch.listener.MetricsStepListener;
import com.practice.apiservice.batch.processor.ImportRecordProcessor;
import com.practice.apiservice.model.ImportRecord;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            @Value("#{jobParameters['chunkSize'] ?: 500}") Integer chunkSize
    ) {
        return new StepBuilder("csvImportStep", jobRepository)
                .<ImportRecord, ImportRecord>chunk(chunkSize, tx)
                .reader(importRecordReader)
                .processor(importRecordProcessor)
                .writer(importRecordWriter)
                .listener(stepListener)
                // NOTE: faultTolerant() lo activaremos en C3 (skip/retry).
                .build();
    }
}
