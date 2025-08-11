package com.practice.apiservice;

import com.practice.apiservice.dto.processing.ProcessingStatusResponse;
import com.practice.apiservice.mapper.ProcessingStatusMapper;
import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingStatusMapperTest {

    ProcessingStatusMapper mapper = Mappers.getMapper(ProcessingStatusMapper.class);

    @Test
    void maps_to_response_with_metrics_and_last() {
        var dfId = UUID.randomUUID();
        var df = new DataFile(dfId, "a.csv", "/x/a.csv", 10, null, Instant.now(), User.ofId(UUID.randomUUID()));
        var config = BatchJobConfig.builder("ETL").chunkSize(100).build();
        var pr = new ProcessingRequest(UUID.randomUUID(), "ETL", df, Map.of("d",";"),
                User.ofId(UUID.randomUUID()), // requestedBy
                config, Instant.now());
        pr.markInProgress();

        var je = new JobExecution(UUID.randomUUID(), pr, Instant.parse("2025-08-10T10:00:00Z"));
        je.finish(com.practice.domain.utils.enums.ExecutionStatus.SUCCESS,
                Instant.parse("2025-08-10T10:05:00Z"),
                100, 95, 5, null);

        ProcessingStatusResponse dto = mapper.toResponse(pr, je);

        assertThat(dto.status()).isEqualTo("IN_PROGRESS");
        assertThat(dto.dataFileId()).isEqualTo(dfId);
        assertThat(dto.metrics().readCount()).isEqualTo(100);
        assertThat(dto.lastExecution().exitStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void maps_to_response_without_last_exec_uses_zero_metrics() {
        var df = new DataFile(UUID.randomUUID(), "a.csv", "/x/a.csv", 10, null, Instant.now(), User.ofId(UUID.randomUUID()));
        var config = BatchJobConfig.builder("ETL").chunkSize(100).build();
        var pr = new ProcessingRequest(UUID.randomUUID(), "ETL", df, Map.of(),
                User.ofId(UUID.randomUUID()), config, Instant.now());

        var dto = mapper.toResponse(pr, null);

        assertThat(dto.metrics().readCount()).isZero();
        assertThat(dto.lastExecution()).isNull();
    }
}