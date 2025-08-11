package com.practice.apiservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.practice.apiservice.entity.DataFileEntity;

public interface DataFileRepository extends JpaRepository<DataFileEntity, UUID> { }
