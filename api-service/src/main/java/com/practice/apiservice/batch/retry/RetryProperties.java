package com.practice.apiservice.batch.retry;

import lombok.Getter;
import lombok.Setter;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("batch.csv.retry")
public class RetryProperties {
    // getters/setters

    /** Número máximo de reintentos (además del primer intento). */
    private int limit = 3;
    /** Backoff inicial. */
    private Duration initial = Duration.ofMillis(200);
    /** Multiplicador del backoff exponencial. */
    private double multiplier = 2.0;
    /** Tope máximo del backoff. */
    private Duration max = Duration.ofSeconds(1);

}
