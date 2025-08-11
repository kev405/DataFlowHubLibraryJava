package com.practice.apiservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        })
class HealthExtraControllerIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void pingDevuelvePong() {
        assertEquals("pong", rest.getForObject("/ping", String.class));
    }
}
