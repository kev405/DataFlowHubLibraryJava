package com.practice.apiservice.batch.processor;

import com.practice.apiservice.model.ImportRecord;
import com.practice.apiservice.batch.processor.RecordValidationException.FieldError;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
@StepScope
public class ImportRecordProcessor implements ItemProcessor<ImportRecord, ImportRecord>, StepExecutionListener, ChunkListener {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final boolean throwOnValidation; // true => lanza excepción; false => filtra (null)
    private final int windowYears;
    private final Clock clock;

    // Estado por Step (gracias a @StepScope)
    private final Set<String> seenIds = new HashSet<>();
    private long row = 0;

    // Constructor por defecto necesario para @StepScope
    public ImportRecordProcessor() {
        this("exception", 2, Clock.systemUTC());
    }

    public ImportRecordProcessor(
            @Value("${batch.processor.validation-mode:exception}") String mode,
            @Value("${batch.processor.event-window-years:2}") int windowYears) {
        this.throwOnValidation = !"filter".equalsIgnoreCase(mode);
        this.windowYears = windowYears;
        this.clock = Clock.systemUTC();
    }

    // ctor alterno para tests si lo necesitas
    public ImportRecordProcessor(String mode, int windowYears, Clock clock) {
        this.throwOnValidation = !"filter".equalsIgnoreCase(mode);
        this.windowYears = windowYears;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void beforeStep(@NonNull StepExecution stepExecution) {
        // Reset state at the beginning of each step
        seenIds.clear();
        row = 0;
    }

    @Override
    public void beforeChunk(@NonNull ChunkContext context) {
        // Reset duplicate detection for each chunk to handle retries correctly
        seenIds.clear();
        row = 0;
    }

    @Override
    public ImportRecord process(@NonNull ImportRecord in) {
        row++; // contador simple por llamada (fila de datos, header ya fue saltado)

        var errors = new ArrayList<FieldError>();

        // --- Validaciones ---
        // externalId
        if (isBlank(in.getExternalId())) {
            errors.add(new FieldError("externalId", "REQUIRED"));
        } else {
            if (!seenIds.add(in.getExternalId())) {
                errors.add(new FieldError("externalId", "DUPLICATED_IN_CHUNK"));
            }
        }

        // userEmail
        if (isBlank(in.getUserEmail())) {
            errors.add(new FieldError("userEmail", "REQUIRED"));
        } else if (!EMAIL.matcher(in.getUserEmail()).matches()) {
            errors.add(new FieldError("userEmail", "INVALID_FORMAT"));
        }

        // amount >= 0
        if (in.getAmount() == null) {
            errors.add(new FieldError("amount", "REQUIRED"));
        } else if (in.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new FieldError("amount", "NEGATIVE"));
        }

        // eventTime: no futuro, dentro de ventana
        Instant now = Instant.now(clock);
        if (in.getEventTime() == null) {
            errors.add(new FieldError("eventTime", "REQUIRED"));
        } else {
            if (in.getEventTime().isAfter(now)) {
                errors.add(new FieldError("eventTime", "FUTURE"));
            }
            // Convertir años a días aproximados (365 días por año)
            long windowDays = windowYears * 365L;
            Instant min = now.minus(windowDays, ChronoUnit.DAYS);
            if (in.getEventTime().isBefore(min)) {
                errors.add(new FieldError("eventTime", "OUT_OF_WINDOW"));
            }
        }

        if (!errors.isEmpty()) {
            if (throwOnValidation) {
                throw new RecordValidationException(row, errors);
            } else {
                return null; // filtrado: cuenta como 'filtered', no como skip
            }
        }

        // --- Transformaciones ---
        var out = new ImportRecord();
        out.setExternalId(in.getExternalId());
        out.setUserEmail(in.getUserEmail().toLowerCase());
        out.setAmount(in.getAmount().setScale(2, RoundingMode.HALF_UP));
        // eventTime ya es Instant (UTC); lo devolvemos tal cual
        out.setEventTime(in.getEventTime());
        out.setMeta(in.getMeta());
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
