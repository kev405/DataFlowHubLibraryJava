package com.practice.apiservice.mapper;

import org.mapstruct.Mapper;
import com.practice.apiservice.entity.ProcessingRequestEntity;
import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.processing.ProcessingRequest;

@Mapper(componentModel = "spring", uses = {DataFileMapper.class, UserMapper.class})
public interface ProcessingRequestMapper {

    default ProcessingRequest toDomain(
            ProcessingRequestEntity e) {

        var pr = new com.practice.domain.processing.ProcessingRequest(
                e.getId(),
                e.getTitle(),
                new com.practice.domain.datafile.DataFile(
                        e.getDataFile().getId(),
                        e.getDataFile().getOriginalFilename(),
                        e.getDataFile().getStoragePath(),
                        e.getDataFile().getSizeBytes(),
                        e.getDataFile().getChecksumSha256(),
                        e.getDataFile().getUploadedAt(),
                        com.practice.domain.user.User.ofId(e.getDataFile().getUploadedBy().getId())
                ),
                e.getParameters(),
                com.practice.domain.user.User.ofId(e.getRequestedBy().getId()),
                new BatchJobConfig(
                BatchJobConfig.builder("OtherJob"
                )),
                e.getCreatedAt()
        );

        switch (e.getStatus()) {
            case PENDING -> {}
            case IN_PROGRESS -> pr.markInProgress();
            case COMPLETED -> { pr.markInProgress(); pr.markCompleted(); }
            case FAILED -> { pr.markInProgress(); pr.markFailed(); }
        }
        return pr;
    }
}
