package com.practice.apiservice.batch.writer;

import com.practice.apiservice.model.ImportRecord;
import java.sql.Timestamp;
import java.util.UUID;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class ImportRecordWriterConfig {

    @Bean
    @StepScope
    public JdbcBatchItemWriter<ImportRecord> importRecordWriter(
            DataSource dataSource,
            @Value("#{jobParameters['processingRequestId']}") String processingRequestId
    ) {
        var npjt = new NamedParameterJdbcTemplate(dataSource);

        String sql = """
        insert into import_records (
            id, processing_request_id, external_id, user_email, amount, event_time
        ) values (
            :id, :processingRequestId, :externalId, :userEmail, :amount, :eventTime
        )
        on conflict (processing_request_id, external_id) do nothing
        """;

        var writer = new JdbcBatchItemWriterBuilder<ImportRecord>()
                .namedParametersJdbcTemplate(npjt)
                .sql(sql)
                // Si hay conflicto, el updateCount puede ser 0; no queremos excepción:
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
