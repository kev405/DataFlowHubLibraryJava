package com.practice.apiservice.batch.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.util.StringUtils;

public class CsvToJpaJobParametersValidator implements JobParametersValidator {

    private static final Set<String> ALLOWED_DELIMITERS = Set.of(",", ";");

    // Toggle para validar existencia del archivo de entrada (dev/prod configurable)
    private final boolean validateStoragePathExists;

    public CsvToJpaJobParametersValidator(boolean validateStoragePathExists) {
        this.validateStoragePathExists = validateStoragePathExists;
    }

    @Override
    public void validate(JobParameters params) throws JobParametersInvalidException {
        Map<String, org.springframework.batch.core.JobParameter<?>> p = params.getParameters();

        var errors = new ArrayList<String>();

        String processingRequestId = str(p.get("processingRequestId"));
        String configId           = str(p.get("configId"));
        String storagePath        = str(p.get("storagePath"));
        String delimiter          = str(p.get("delimiter")); // opcional
        Long chunkSize            = lng(p.get("chunkSize")); // opcional

        // Requeridos
        if (!StringUtils.hasText(processingRequestId)) errors.add("processingRequestId is required");
        if (!StringUtils.hasText(configId))           errors.add("configId is required");
        if (!StringUtils.hasText(storagePath))        errors.add("storagePath is required");

        // Formato UUID
        if (StringUtils.hasText(processingRequestId) && !isUuid(processingRequestId)) {
            errors.add("processingRequestId must be a valid UUID");
        }

        // Delimiter permitido
        if (StringUtils.hasText(delimiter) && !ALLOWED_DELIMITERS.contains(delimiter)) {
            errors.add("delimiter must be one of " + ALLOWED_DELIMITERS);
        }

        // chunkSize en rango
        if (chunkSize != null && (chunkSize < 100 || chunkSize > 10_000)) {
            errors.add("chunkSize must be in range [100..10000]");
        }

        // Existencia de archivo (opcional según toggle)
        if (validateStoragePathExists && StringUtils.hasText(storagePath)) {
            try {
                if (!Files.exists(Path.of(storagePath))) {
                    errors.add("storagePath does not exist: " + storagePath);
                } else if (!Files.isReadable(Path.of(storagePath))) {
                    errors.add("storagePath is not readable: " + storagePath);
                }
            } catch (Exception e) {
                errors.add("storagePath check failed: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new JobParametersInvalidException(String.join("; ", errors));
        }
    }

    private static String str(org.springframework.batch.core.JobParameter<?> p) {
        return p == null ? null : (String) p.getValue();
    }

    private static Long lng(org.springframework.batch.core.JobParameter<?> p) {
        return p == null ? null : (Long) p.getValue();
    }

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; } catch (Exception ex) { return false; }
    }
}
