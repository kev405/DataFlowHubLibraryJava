package com.practice.apiservice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.practice.apiservice.batch.processor.RecordValidationException;
import com.practice.apiservice.batch.skip.ImportErrorSink;
import com.practice.apiservice.batch.skip.ImportSkipListener;
import com.practice.apiservice.model.ImportRecord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.batch.job.enabled=false",
        "spring.flyway.enabled=false",                // 👈 que no migre en este IT
        "spring.batch.jdbc.initialize-schema=always", // 👈 que Spring Batch cree su schema
        "spring.batch.jdbc.platform=postgresql",
        "batch.csv.skip-limit=10"
})
@SpringBatchTest
class ImportSkipListenerTest {

    @Autowired
    ImportErrorSink sink;

    @Autowired
    ApplicationContext ctx;

    @MockBean
    NamedParameterJdbcTemplate jdbc;

    @Test
    void onSkipInProcess_persists_error() {
        var l = ctx.getBean(ImportSkipListener.class);
        ReflectionTestUtils.setField(l, "processingRequestId",
                UUID.fromString("11111111-1111-1111-1111-111111111111"));

        var item = new ImportRecord("A-1","a@x.com", BigDecimal.ONE, Instant.now());
        var errors = new ArrayList<RecordValidationException.FieldError>();
        errors.add(new RecordValidationException.FieldError("email", "invalid email format"));
        l.onSkipInProcess(item, new RecordValidationException(10L, errors));

        verify(jdbc).update(anyString(), any(MapSqlParameterSource.class));
    }
}

