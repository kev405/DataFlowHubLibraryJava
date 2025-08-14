package com.practice.apiservice.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig extends DefaultBatchConfiguration {

    private final DataSource dataSource;
    private final PlatformTransactionManager txManager;

    @Override protected DataSource getDataSource() { return dataSource; }
    @Override protected PlatformTransactionManager getTransactionManager() { return txManager; }

}
