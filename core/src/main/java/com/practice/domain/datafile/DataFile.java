package com.practice.domain.datafile;

import com.practice.domain.user.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A file stored in the system.
 *
 * <p>Modeled as a {@code record} ⇒ immutable, thread-safe,
 * and with generated {@code equals}, {@code hashCode}, and {@code toString}.</p>
 */
public record DataFile(
        UUID   id,                // global identifier
        String originalFilename,  // filename as provided by the client
        String storagePath,       // final path (disk, S3, …)
        long   sizeBytes,         // must be &gt; 0
        String checksumSha256,    // 64 hex characters
        Instant uploadedAt,       // timestamp of upload
        User   uploadedBy         // user who performed the upload
) {

    public static DataFile createForUpload(
            String originalFilename,
            String storagePath,
            long sizeBytes,
            String checksumSha256,
            UUID uploadedByUserId
    ) {
        return new DataFile(
                UUID.randomUUID(),
                originalFilename,
                storagePath,
                sizeBytes,
                checksumSha256,
                Instant.now(),
                User.ofId(uploadedByUserId) // ver helper abajo
        );
    }

    public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    /** Validates business rules upon construction. */
    public DataFile {
        Objects.requireNonNull(id);
        Objects.requireNonNull(originalFilename);
        Objects.requireNonNull(storagePath);
        Objects.requireNonNull(uploadedAt);
        Objects.requireNonNull(uploadedBy);

        if (originalFilename.isBlank())
            throw new IllegalArgumentException("originalFilename is blank");

        if (storagePath.isBlank())
            throw new IllegalArgumentException("storagePath is blank");

        if (sizeBytes <= 0)
            throw new IllegalArgumentException("sizeBytes must be > 0");

        if (checksumSha256 != null && !checksumSha256.matches("^[0-9a-fA-F]{64}$"))
            throw new IllegalArgumentException(
                    "checksumSha256 must contain exactly 64 hexadecimal characters");
    }
}
