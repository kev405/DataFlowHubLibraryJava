package com.practice.apiservice.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import com.practice.apiservice.dto.FileUploadRequest;
import com.practice.apiservice.dto.FileUploadResponse;
import com.practice.apiservice.utils.error.FileTooLargeException;
import com.practice.domain.datafile.DataFile;

@RestController
@RequestMapping("/files")
public class FileController {

    @PostMapping
    public ResponseEntity<FileUploadResponse> upload(@Valid @RequestBody FileUploadRequest r) {

        if (r.sizeBytes() > DataFile.MAX_SIZE_BYTES) {
            throw new FileTooLargeException(DataFile.MAX_SIZE_BYTES, r.sizeBytes());
        }

        var df = DataFile.createForUpload(
                r.originalFilename(), r.storagePath(), r.sizeBytes(), r.checksumSha256(), r.uploadedByUserId()
        );

        var body = new FileUploadResponse(df.id(), df.originalFilename());
        return ResponseEntity.created(URI.create("/files/" + df.id())).body(body);
    }
}
