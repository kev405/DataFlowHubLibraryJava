package com.practice.apiservice.batch;

import com.practice.apiservice.batch.listener.LoggingJobExecutionListener;
import com.practice.apiservice.batch.listener.MetricsStepListener;
import com.practice.apiservice.batch.processor.ImportRecordProcessor;
import com.practice.apiservice.batch.validation.CsvToJpaJobParametersValidator;
import com.practice.apiservice.model.ImportRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DemoJobConfig {

    @Bean
    CsvToJpaJobParametersValidator csvToJpaValidator(
            @Value("${batch.validate.storage-path-exists:false}") boolean checkPath
    ) {
        return new CsvToJpaJobParametersValidator(checkPath);
    }

    @Bean
    public Job demoJob(JobRepository jobRepository,
                       Step demoStep,
                       LoggingJobExecutionListener jobListener,
                       CsvToJpaJobParametersValidator validator,
                       JobParametersIncrementer incrementer) {
        return new JobBuilder("demoJob", jobRepository)
                .validator(validator)
                .listener(jobListener)
                .incrementer(incrementer)
                .start(demoStep)
                .build();
    }

    @Bean
    public Step demoStep(JobRepository jobRepository,
                         PlatformTransactionManager tx,
                         MetricsStepListener stepListener) {
        return new StepBuilder("demoStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, tx)
                .listener(stepListener)
                .build();
    }

    @Bean
    public Step csvToJpaStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             FlatFileItemReader<ImportRecord> importRecordReader,
                             ImportRecordProcessor importRecordProcessor,
                             JdbcBatchItemWriter<ImportRecord> importRecordWriter,
                             MetricsStepListener stepListener) {

        int chunkSize = 500; // o léelo desde JobParameters/propiedades

        return new StepBuilder("csvToJpaStep", jobRepository)
                .<ImportRecord, ImportRecord>chunk(chunkSize, tx)
                .reader(importRecordReader)
                .processor(importRecordProcessor)
                .writer(importRecordWriter)
                .listener(stepListener)
                // Si el processor lanza excepciones de validación:
                // .faultTolerant().skip(RecordValidationException.class).skipLimit(1000)
                .build();
    }

//    @Bean
//    @StepScope
//    public JpaItemWriter<ImportRecordEntity> importRecordJpaWriter(EntityManagerFactory emf) {
//        JpaItemWriter<ImportRecordEntity> w = new JpaItemWriter<>();
//        w.setEntityManagerFactory(emf);
//        w.setUsePersist(true); // batch insert con hibernate.jdbc.batch_size
//        return w;
//    }
}
