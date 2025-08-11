package com.practice.apiservice.mapper;

import com.practice.apiservice.entity.*;
import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.RequestStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface EntityToDomainMapper {

    // ---- básicos ----
    default User toDomain(UserEntity e) {
        return User.ofId(e.getId());
    }

    @Mapping(target = "uploadedBy", expression = "java(User.ofId(e.getUploadedBy().getId()))")
    DataFile toDomain(DataFileEntity e);

    default BatchJobConfig toDomain(BatchJobConfigEntity e) {
        // El core tiene builder con logicalName; lo reconstruimos mínimamente.
        return BatchJobConfig.builder(e.getName())
                .description(e.getDescription())
                .chunkSize(e.getChunkSize())
                .readerType(e.getReaderType())
                .writerType(e.getWriterType())
                .createdAt(e.getCreatedAt())
                .build();
    }

    // ---- ProcessingRequest: arranca en PENDING y aplicamos transición si hace falta ----
    default ProcessingRequest toDomain(ProcessingRequestEntity e) {
        var pr = new ProcessingRequest(
                e.getId(),
                e.getTitle(),
                toDomain(e.getDataFile()),
                safeParams(e.getParameters()),
                toDomain(e.getRequestedBy()),
                toDomain(e.getBatchJobConfig()),
                e.getCreatedAt()
        );
        if (e.getStatus() != null && e.getStatus() != RequestStatus.PENDING) {
            switch (e.getStatus()) {
                case IN_PROGRESS -> pr.markInProgress();
                case COMPLETED   -> { pr.markInProgress(); pr.markCompleted(); }
                case FAILED      -> { pr.markInProgress(); pr.markFailed(); }
                default -> {} // PENDING ya aplicado
            }
        }
        return pr;
    }

    default JobExecution toDomain(JobExecutionEntity e) {
        var pr = toDomain(e.getProcessingRequest());
        var je = new JobExecution(e.getId(), pr, e.getStartTime());
        if (e.getExitStatus() != null) {
            je.finish(e.getExitStatus(), e.getEndTime(),
                    e.getReadCount(), e.getWriteCount(), e.getSkipCount(),
                    e.getErrorMessage());
        }
        return je;
    }

    private static Map<String,String> safeParams(Map<String,String> p) {
        return (p == null ? Map.of() : p);
    }
}
