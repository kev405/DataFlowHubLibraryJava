package com.practice.apiservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import com.practice.apiservice.entity.JobExecutionEntity;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {

    // Si el campo en la entidad es `processingRequest` (ManyToOne),
    // Spring Data permite navegar por id así:
    Optional<JobExecutionEntity> findTop1ByProcessingRequestIdOrderByStartTimeDesc(UUID processingRequestId);
}
