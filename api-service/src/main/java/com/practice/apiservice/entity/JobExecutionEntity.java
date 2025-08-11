package com.practice.apiservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.practice.domain.utils.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobExecutionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="processing_request_id", columnDefinition="uuid")
    private ProcessingRequestEntity processingRequest;

    @Column(nullable=false)
    private Instant startTime;

    private Instant endTime;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus exitStatus;

    private long readCount;

    private long writeCount;

    private long skipCount;

    @Column(length=1000)
    private String errorMessage;

}
