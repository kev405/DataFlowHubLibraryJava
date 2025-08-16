package com.practice.apiservice.batch.skip;

import com.practice.apiservice.batch.processor.RecordValidationException;
import com.practice.apiservice.model.ImportRecord;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ImportSkipListener implements SkipListener<ImportRecord, ImportRecord> {

    private final ImportErrorSink sink;

    @Value("#{jobParameters['processingRequestId']}")
    private UUID processingRequestId;

    @Value("${batch.csv.persist-errors:true}")
    private boolean persist;

    @Override
    public void onSkipInRead(Throwable t) {
        Long row = null; String raw = null;
        if (t instanceof FlatFileParseException fpe) {
            row = (long) fpe.getLineNumber();
            raw = fpe.getInput();
        }
        log.warn("skip_in_read event=CSV_PARSE_ERROR req={} row={} msg={} raw={}",
                processingRequestId, row, t.getMessage(), raw);
        if (persist) sink.save(processingRequestId, row, null, "CSV_PARSE_ERROR: " + t.getMessage(), raw);
    }

    @Override
    public void onSkipInProcess(ImportRecord item, Throwable t) {
        Long row = null;
        String reason = t.getMessage();
        if (t instanceof RecordValidationException rve) {
            row = rve.getRowNumber();
            // si tienes errores detalle, puedes formatearlos aquí
            reason = "VALIDATION_ERROR: " + rve.getMessage();
        }
        log.info("skip_in_process event=VALIDATION req={} row={} ext={} reason={}",
                processingRequestId, row, item != null ? item.getExternalId() : null, reason);
        if (persist) sink.save(processingRequestId, row,
                item != null ? item.getExternalId() : null, reason, null);
    }

    @Override
    public void onSkipInWrite(ImportRecord item, Throwable t) {
        String ext = item != null ? item.getExternalId() : null;
        log.warn("skip_in_write event=WRITE_ERROR req={} ext={} msg={}",
                processingRequestId, ext, t.getMessage());
        if (persist) sink.save(processingRequestId, null, ext, "WRITE_ERROR: " + t.getMessage(), null);
    }
}
