package com.practice.apiservice.batch.incrementer;

import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Permite elegir estrategia vía propiedad 'batch.incrementer' */
@Configuration
public class IncrementerConfig {

    @Bean
    @ConditionalOnProperty(name = "batch.incrementer", havingValue = "runId", matchIfMissing = true)
    public JobParametersIncrementer runIdIncrementer() {
        return new RunIdIncrementer(); // añade/actualiza run.id (Long)
    }

    @Bean
    @ConditionalOnProperty(name = "batch.incrementer", havingValue = "requestTime")
    public JobParametersIncrementer requestTimeIncrementer() {
        return new RequestTimeIncrementer(); // añade/actualiza requestTime (Date)
    }
}
