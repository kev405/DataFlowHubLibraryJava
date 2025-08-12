package com.practice.apiservice.batch;

import java.util.List;

public interface PendingProcessingProvider {
    List<ProcessingItem> findPending(int limit);

    record ProcessingItem(String configId, String processingRequestId) {}
}
