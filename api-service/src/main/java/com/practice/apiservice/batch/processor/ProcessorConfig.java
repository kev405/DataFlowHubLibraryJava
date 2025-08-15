package com.practice.apiservice.batch.processor;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig {
    // Los beans @Component/@StepScope ya se registran:
    // - ImportRecordProcessor
    // - LoggingProcessListener
}
