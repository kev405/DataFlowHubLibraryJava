package com.practice.apiservice.rest;

import com.practice.apiservice.config.AppBatchProps;
import com.practice.apiservice.dto.processing.CreateProcessingRequest;
import com.practice.apiservice.dto.processing.ProcessingCreatedResponse;
import com.practice.apiservice.exception.RestExceptionHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateProcessingRequest r, BindingResult br) {
        var title = r.title().trim();
        if (title.isEmpty())         br.rejectValue("title", "NotBlank", "must not be blank");
        else if (title.length() > 140) br.rejectValue("title", "Size", "length must be <= 140 after trimming");
        if (br.hasErrors()) return RestExceptionHandler.badRequestFrom(br);

        UUID effectiveCfgId = Optional.ofNullable(r.batchJobConfigId()).orElseGet(props::defaultConfigId);

        UUID processingId = UUID.randomUUID();

        // log.info("ProcessingRequest received: title='{}', dataFileId={}, requestedBy={}, cfgId={}",
        //          title, r.dataFileId(), r.requestedByUserId(), effectiveCfgId);

        return ResponseEntity.accepted()
                .location(URI.create("/processings/" + processingId))
                .body(new ProcessingCreatedResponse(processingId, "PENDING"));
    }
}