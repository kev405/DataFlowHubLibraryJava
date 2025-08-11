package com.practice.apiservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.apiservice.config.AppBatchProps;
import com.practice.apiservice.dto.processing.CreateProcessingRequest;
import com.practice.apiservice.exception.RestExceptionHandler;
import com.practice.apiservice.rest.ProcessingController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProcessingController.class)
@Import(RestExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ProcessingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean AppBatchProps props;

    @Test
    void accepted202_withLocation_andBody() throws Exception {
        var req = new CreateProcessingRequest(
                "  ETL Ventas Julio  ",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                Map.of("delimiter", ";")
        );

        Mockito.when(props.defaultConfigId())
                .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mvc.perform(post("/processings")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(req)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", startsWith("/processings/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")));

        Mockito.verify(props).defaultConfigId();
    }

    @Test
    void usesProvidedBatchConfigId_withoutTouchingDefault() throws Exception {
        var req = new CreateProcessingRequest(
                "ETL Ventas", UUID.randomUUID(), UUID.randomUUID(),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null
        );

        mvc.perform(post("/processings")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(req)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", startsWith("/processings/")))
                .andExpect(jsonPath("$.status", is("PENDING")));

        Mockito.verify(props, Mockito.never()).defaultConfigId();
    }

    @Test
    void rejects_when_title_trimmed_is_blank_or_length_gt_140() throws Exception {
        var bad1 = """
      { "title":"   ", "dataFileId":"%s", "requestedByUserId":"%s" }
      """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/processings")
                        .contentType(APPLICATION_JSON)
                        .content(bad1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].field", hasItem("title")));

        var longTitle = " " + "A".repeat(141) + " ";
        var bad2 = new CreateProcessingRequest(
                longTitle, UUID.randomUUID(), UUID.randomUUID(), null, null
        );

        mvc.perform(post("/processings")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(bad2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].field", hasItem("title")));
    }
}