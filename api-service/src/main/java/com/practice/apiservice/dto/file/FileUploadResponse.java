package com.practice.apiservice.dto.file;

import java.util.UUID;

public record FileUploadResponse(UUID id, String originalFilename) {}
