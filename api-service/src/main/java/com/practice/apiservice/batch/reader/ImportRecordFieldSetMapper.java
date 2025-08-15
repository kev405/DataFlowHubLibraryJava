package com.practice.apiservice.batch.reader;

import com.practice.apiservice.model.ImportRecord;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;

public class ImportRecordFieldSetMapper implements FieldSetMapper<ImportRecord> {

    @Override
    public ImportRecord mapFieldSet(FieldSet fs) {
        String externalId = emptyToNull(fs.readString("external_id"));
        String userEmail  = emptyToNull(fs.readString("user_email"));

        String amountStr  = emptyToNull(fs.readString("amount"));
        BigDecimal amount = (amountStr == null) ? null : new BigDecimal(amountStr);

        String eventStr   = emptyToNull(fs.readString("event_time"));
        Instant eventTime = (eventStr == null) ? null : Instant.parse(eventStr); // ISO-8601

        return new ImportRecord(externalId, userEmail, amount, eventTime);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
