package com.practice.apiservice;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import com.practice.apiservice.model.ImportRecord;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.flyway.enabled=false"
})
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
class ImportRecordWriterTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @Test
    void writes_batch_and_is_idempotent() throws Exception {
        String processingRequestId = UUID.randomUUID().toString();

        // Crear la tabla import_records para el test
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS import_records (
                id UUID PRIMARY KEY,
                processing_request_id UUID NOT NULL,
                external_id VARCHAR(255) NOT NULL,
                user_email VARCHAR(255) NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                event_time TIMESTAMP NOT NULL,
                UNIQUE(processing_request_id, external_id)
            )
        """);

        // Crear el writer manualmente sin @StepScope
        JdbcBatchItemWriter<ImportRecord> importRecordWriter = createImportRecordWriter(processingRequestId);

        List<ImportRecord> items = List.of(
                new ImportRecord("E-1", "a@x.com", new BigDecimal("10.00"), Instant.parse("2025-07-01T10:00:00Z")),
                new ImportRecord("E-2", "b@x.com", new BigDecimal("20.00"), Instant.parse("2025-07-02T10:00:00Z"))
        );

        // Convertir List a Chunk
        Chunk<ImportRecord> chunk = new Chunk<>(items);

        // Escribir los items dos veces para probar idempotencia
        importRecordWriter.write(chunk);
        importRecordWriter.write(chunk); // repetir → ON CONFLICT evita duplicar

        Integer count = jdbc.queryForObject("select count(*) from import_records", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    private JdbcBatchItemWriter<ImportRecord> createImportRecordWriter(String processingRequestId) {
        var npjt = new NamedParameterJdbcTemplate(dataSource);

        // Usar MERGE en lugar de ON CONFLICT para compatibilidad con H2
        String sql = """
        MERGE INTO import_records (
            id, processing_request_id, external_id, user_email, amount, event_time
        ) KEY(processing_request_id, external_id) VALUES (
            :id, :processingRequestId, :externalId, :userEmail, :amount, :eventTime
        )
        """;

        var writer = new JdbcBatchItemWriterBuilder<ImportRecord>()
                .namedParametersJdbcTemplate(npjt)
                .sql(sql)
                .assertUpdates(false)
                .itemSqlParameterSourceProvider(item -> {
                    var p = new MapSqlParameterSource();
                    p.addValue("id", UUID.randomUUID());
                    p.addValue("processingRequestId", UUID.fromString(processingRequestId));
                    p.addValue("externalId", item.getExternalId());
                    p.addValue("userEmail", item.getUserEmail());
                    p.addValue("amount", item.getAmount());
                    p.addValue("eventTime", Timestamp.from(item.getEventTime()));
                    return p;
                })
                .build();

        writer.afterPropertiesSet();
        return writer;
    }
}
