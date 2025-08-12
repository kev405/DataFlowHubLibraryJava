package com.practice.apiservice.dto.batch;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RunJobRequest(
        @NotBlank String processingRequestId,
        Map<String,String> parameters
) {}
