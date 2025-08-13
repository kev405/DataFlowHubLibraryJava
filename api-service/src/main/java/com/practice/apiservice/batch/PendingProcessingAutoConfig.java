package com.practice.apiservice.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PendingProcessingAutoConfig {

    @Bean
    @ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
    @ConditionalOnMissingBean(PendingProcessingProvider.class)
    PendingProcessingProvider noopPendingProcessingProvider() {
        return limit -> List.of(); // no hay pendientes ⇒ scheduler no lanza nada
    }
}
