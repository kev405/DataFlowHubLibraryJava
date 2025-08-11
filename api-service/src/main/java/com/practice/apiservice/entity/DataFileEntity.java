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

@Entity
@Table(name = "data_files")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataFileEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable=false, length=255)
    private String originalFilename;

    @Column(nullable=false, length=1024)
    private String storagePath;

    @Column(nullable=false)
    private long sizeBytes;

    @Column(length=64)
    private String checksumSha256;

    @Column(nullable=false)
    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", columnDefinition = "uuid")
    private UserEntity uploadedBy;

}
