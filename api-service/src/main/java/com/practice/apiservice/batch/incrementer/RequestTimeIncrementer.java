package com.practice.apiservice.batch.incrementer;

import java.time.Instant;
import java.util.Date;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

/**
 * Agrega/actualiza un parámetro identificante 'requestTime' (Date) para forzar nueva JobInstance.
 */
public class RequestTimeIncrementer implements JobParametersIncrementer {
    @Override
    public JobParameters getNext(JobParameters parameters) {
        return new JobParametersBuilder(parameters == null ? new JobParameters() : parameters)
                // 'identifying=true' para que participe en la identidad del JobInstance
                .addDate("requestTime", Date.from(Instant.now()), true)
                .toJobParameters();
    }
}
