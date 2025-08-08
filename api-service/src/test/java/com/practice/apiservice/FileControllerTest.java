package com.practice.apiservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.apiservice.dto.FileUploadRequest;
import com.practice.apiservice.rest.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileController.class)
class FileControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void created201_withLocation_andBody() throws Exception {
        var req = new FileUploadRequest("ventas_julio.csv", 1048576L, null,
                "/data/in/ventas_julio.csv", UUID.randomUUID());

        mvc.perform(post("/files").contentType(APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/files/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.originalFilename", is("ventas_julio.csv")));
    }

    @Test
    void reject_over50mb() throws Exception {
        var req = new FileUploadRequest("big.bin", 50L*1024*1024 + 1, null, "/tmp/big.bin", UUID.randomUUID());

        mvc.perform(post("/files").contentType(APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FILE_TOO_LARGE")));
    }

    @Test
    void reject_blank_and_bad_checksum() throws Exception {
        var bad = """
      {
        "originalFilename": " ",
        "sizeBytes": 100,
        "checksumSha256": "XYZ",
        "storagePath": "/data/in/a.csv",
        "uploadedByUserId": "%s"
      }
      """.formatted(UUID.randomUUID());

        mvc.perform(post("/files").contentType(APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].field", hasItems("originalFilename","checksumSha256")));
    }
}
