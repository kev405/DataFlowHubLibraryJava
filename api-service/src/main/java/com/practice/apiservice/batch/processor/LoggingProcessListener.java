package com.practice.apiservice.batch.processor;

import com.practice.apiservice.model.ImportRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.stereotype.Component;

@Component
public class LoggingProcessListener implements ItemProcessListener<ImportRecord, ImportRecord> {
    private static final Logger log = LoggerFactory.getLogger(LoggingProcessListener.class);

    @Override public void beforeProcess(ImportRecord item) { /* no-op */ }

    @Override public void afterProcess(ImportRecord item, ImportRecord result) {
        if (result == null) {
            log.debug("Filtered record: externalId={}", item.getExternalId());
        }
    }

    @Override public void onProcessError(ImportRecord item, Exception e) {
        log.warn("Validation error on externalId={}: {}", item.getExternalId(), e.getMessage());
    }
}
