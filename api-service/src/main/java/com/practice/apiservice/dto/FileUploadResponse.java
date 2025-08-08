package com.practice.apiservice.dto;

import java.util.UUID;

public record FileUploadResponse(UUID id, String originalFilename) {}
