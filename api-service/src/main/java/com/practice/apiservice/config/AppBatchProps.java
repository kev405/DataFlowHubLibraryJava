package com.practice.apiservice.config;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch")
public record AppBatchProps(UUID defaultConfigId) {}
