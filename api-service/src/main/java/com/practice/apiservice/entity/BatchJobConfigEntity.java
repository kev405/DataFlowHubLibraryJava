package com.practice.apiservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.practice.domain.utils.enums.ReaderType;
import com.practice.domain.utils.enums.WriterType;

@Entity
@Table(name = "batch_job_configs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchJobConfigEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable=false, length=140)
    private String name;

    @Column(nullable=false)
    private String description = "";

    @Column(nullable=false)
    private int chunkSize = 1000;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ReaderType readerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private WriterType writerType;

    @Column(nullable=false)
    private boolean allowRestart = false;

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private boolean active = true;

}