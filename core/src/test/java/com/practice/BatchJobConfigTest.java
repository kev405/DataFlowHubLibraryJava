package com.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.practice.domain.batchconfig.BatchJobConfig;

import nl.jqno.equalsverifier.EqualsVerifier;

/** Contract tests for {@link BatchJobConfig}. */
public class BatchJobConfigTest {
    
    @Test
    void equalsAndHashCode_areBasedOnlyOnId() {
        EqualsVerifier.forClass(BatchJobConfig.class)
            .withNonnullFields("id")
            .withOnlyTheseFields("id")
            .verify();
    }

     @Test
    void builder_createsValidDefaultConfig() {
        BatchJobConfig cfg = BatchJobConfig.builder("ETL-Users").build();
        assertTrue(cfg.isActive());
        assertFalse(cfg.allowRestart());
        assertEquals(1000, cfg.chunkSize());           // default value
    }

    @Test
    void builder_rejectsInvalidChunkSize() {
        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class,
                () -> BatchJobConfig.builder("bad").chunkSize(0).build());
        assertTrue(ex.getMessage().contains("chunkSize"));
    }
}
