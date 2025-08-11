package com.practice.apiservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.practice.apiservice.map.MapToJsonConverter;
import com.practice.domain.utils.enums.RequestStatus;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "processing_requests")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessingRequestEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable=false, length=140)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="data_file_id", columnDefinition = "uuid")
    private DataFileEntity dataFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable=false)
    private Map<String,String> parameters;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private RequestStatus status;

    @Column(nullable=false) private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="requested_by_id", columnDefinition = "uuid")
    private UserEntity requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="batch_job_config_id", columnDefinition = "uuid")
    private BatchJobConfigEntity batchJobConfig;

}
