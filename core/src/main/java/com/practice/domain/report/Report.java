package com.practice.domain.report;

import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Business-level artifact produced after a {@link ProcessingRequest} finishes.
 * <p>
 * Implementado como <strong>record</strong> (Java 17) ⇒ inmutable,
 * <code>equals</code>/<code>hashCode</code>/<code>toString</code> generados.
 * </p>
 */
public record Report(
        UUID                id,                 // identidad global
        ProcessingRequest   processingRequest,  // request origen (≠ null)
        String              storagePath,        // ruta en disco / S3
        String              summaryJson,        // JSON compacto con métricas
        Instant             generatedAt,        // ≥ request.createdAt
        User                generatedBy         // usuario que disparó el reporte
) {

    /** Compact constructor con reglas de negocio básicas. */
    public Report {
        Objects.requireNonNull(id);
        Objects.requireNonNull(processingRequest);
        Objects.requireNonNull(storagePath);
        Objects.requireNonNull(summaryJson);
        Objects.requireNonNull(generatedAt);
        Objects.requireNonNull(generatedBy);

        if (storagePath.isBlank())
            throw new IllegalArgumentException("storagePath is blank");
        if (summaryJson.isBlank())
            throw new IllegalArgumentException("summaryJson is blank");
        if (generatedAt.isBefore(processingRequest.getCreatedAt()))
            throw new IllegalArgumentException(
                    "generatedAt must be after the request creation time");
    }
}
