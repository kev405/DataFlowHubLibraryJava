package com.practice.apiservice.rest;

import com.practice.apiservice.config.AppBatchProps;
import com.practice.apiservice.dto.processing.CreateProcessingRequest;
import com.practice.apiservice.dto.processing.ProcessingCreatedResponse;
import com.practice.apiservice.exception.ApiErrorHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/processings")
public class ProcessingController {

    private final AppBatchProps props;
    public ProcessingController(AppBatchProps props) { this.props = props; }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProcessingRequest r, BindingResult br) {
        // Reglas extra: trim + longitud 1..140
        var title = r.title().trim();
        if (title.isEmpty())         br.rejectValue("title", "NotBlank", "must not be blank");
        else if (title.length() > 140) br.rejectValue("title", "Size", "length must be <= 140 after trimming");
        if (br.hasErrors()) return ApiErrorHandler.badRequestFrom(br);

        // Elegir el batchJobConfigId efectivo (request o default desde yml)
        UUID effectiveCfgId = Optional.ofNullable(r.batchJobConfigId()).orElseGet(props::defaultConfigId);

        // F2-05: SOLO acuse de recibo (no instanciamos dominio porque faltan repos/lookups)
        UUID processingId = UUID.randomUUID();

        // (opcional) log para rastrear que título, fileId, requester y cfg se recibieron
        // log.info("ProcessingRequest received: title='{}', dataFileId={}, requestedBy={}, cfgId={}",
        //          title, r.dataFileId(), r.requestedByUserId(), effectiveCfgId);

        return ResponseEntity.accepted()
                .location(URI.create("/processings/" + processingId))
                .body(new ProcessingCreatedResponse(processingId, "PENDING"));
    }
}