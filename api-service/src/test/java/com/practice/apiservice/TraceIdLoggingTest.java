package com.practice.apiservice;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
class TraceIdLoggingTest {

    @Autowired MockMvc mvc;
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Get the logger for the HealthExtraController
        logger = (Logger) LoggerFactory.getLogger("com.practice.apiservice.rest.HealthExtraController");
        
        // Set the logger level to INFO to ensure log capture
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        
        // Create and start the list appender
        listAppender = new ListAppender<>();
        listAppender.start();
        
        // Add the appender to the logger
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // Clean up the appender
        if (logger != null && listAppender != null) {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    void traceIdFilterSetsTraceIdInMdc() throws Exception {
        // Call the ping endpoint which logs "Ping received"
        var result = mvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andReturn();
        
        // Verify the response is correct
        assertThat(result.getResponse().getContentAsString()).isEqualTo("pong");
        
        // Verify that we captured log events
        assertThat(listAppender.list).isNotEmpty();
        
        // Find the "Ping received" log event
        var pingLogEvent = listAppender.list.stream()
                .filter(event -> "Ping received".equals(event.getMessage()))
                .findFirst();
        
        assertThat(pingLogEvent).isPresent();
        
        // Verify that the log event has MDC properties with trace ID
        Map<String, String> mdcProperties = pingLogEvent.get().getMDCPropertyMap();
        assertThat(mdcProperties).isNotNull();
        assertThat(mdcProperties).containsKey("traceId");
        assertThat(mdcProperties).containsKey("spanId");
        
        // Verify trace ID format (should be a UUID)
        String traceId = mdcProperties.get("traceId");
        assertThat(traceId).isNotNull();
        assertThat(traceId).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        
        // Verify span ID format (should be first 16 characters of a UUID: 8 chars + hyphen + 4 chars + 3 chars)
        String spanId = mdcProperties.get("spanId");
        assertThat(spanId).isNotNull();
        assertThat(spanId).hasSize(16);
        assertThat(spanId).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{2}$");
    }
}
