package com.practice.apiservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.practice.apiservice.entity.ReportEntity;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> { }