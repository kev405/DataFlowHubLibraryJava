package com.practice.apiservice.mapper;

import com.practice.apiservice.dto.processing.ProcessingStatusResponse;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProcessingStatusMapper {

    @Mapping(target = "status", expression = "java(pr.status().name())")
    @Mapping(target = "dataFileId", expression = "java(pr.dataFile().id())")
    @Mapping(target = "metrics", expression = "java(toMetrics(last))")
    @Mapping(target = "lastExecution", expression = "java(toLast(last))")
    ProcessingStatusResponse toResponse(ProcessingRequest pr, JobExecution last);

    // Helpers (MapStruct los invoca desde las expressions)
    default ProcessingStatusResponse.Metrics toMetrics(JobExecution last) {
        if (last == null) return new ProcessingStatusResponse.Metrics(0,0,0);
        return new ProcessingStatusResponse.Metrics(last.readCount(), last.writeCount(), last.skipCount());
    }

    default ProcessingStatusResponse.LastExecution toLast(JobExecution last) {
        if (last == null) return null;
        var exit = (last.exitStatus() == null ? null : last.exitStatus().name());
        return new ProcessingStatusResponse.LastExecution(
                last.startTime(), last.endTime(), exit, last.errorMessage());
    }
}
