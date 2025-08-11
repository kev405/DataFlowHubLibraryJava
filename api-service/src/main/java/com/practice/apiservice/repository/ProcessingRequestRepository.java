package com.practice.apiservice.repository;

import com.practice.apiservice.entity.ProcessingRequestEntity;
import com.practice.domain.utils.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessingRequestRepository
        extends JpaRepository<ProcessingRequestEntity, UUID> {

    Page<ProcessingRequestEntity> findByStatus(RequestStatus status, Pageable pageable);
}
