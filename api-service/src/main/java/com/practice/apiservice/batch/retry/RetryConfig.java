package com.practice.apiservice.batch.retry;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

@Configuration
@EnableConfigurationProperties(RetryProperties.class)
public class RetryConfig {
    @Bean
    ExponentialBackOffPolicy csvRetryBackoff(RetryProperties p) {
        var backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(p.getInitial().toMillis());
        backoff.setMultiplier(p.getMultiplier());
        backoff.setMaxInterval(p.getMax().toMillis());
        return backoff;
    }
}
