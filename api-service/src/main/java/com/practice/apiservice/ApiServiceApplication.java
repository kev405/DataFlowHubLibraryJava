package com.practice.apiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.practice.apiservice.config.AppBatchProps;

@SpringBootApplication(scanBasePackages = "com.practice")
@EnableConfigurationProperties(AppBatchProps.class)
public class ApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }

}
