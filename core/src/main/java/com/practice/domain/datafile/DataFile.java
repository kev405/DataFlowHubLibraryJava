package com.practice.domain.datafile;

import java.time.Instant;
import java.util.UUID;

import com.practice.domain.user.User;

public record DataFile(
      UUID id,
      String originalFilename,
      String storagePath,
      long sizeBytes,
      String checksumSha256,
      Instant uploadedAt,
      User uploadedBy
) {
   public DataFile {
       if (sizeBytes <= 0) throw new IllegalArgumentException("sizeBytes <= 0");
       requireSha256(checksumSha256);
   }

   private static void requireSha256(String checksum) {
       if (checksum == null || !checksum.matches("^[a-fA-F0-9]{64}$")) {
           throw new IllegalArgumentException("Invalid SHA-256 checksum");
       }
   }
}

