package com.practice.apiservice;

import com.practice.apiservice.exception.RestExceptionHandler;
import com.practice.apiservice.utils.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RestExceptionHandlerTest {

    private MockMvc mvc;

    // --- DTO para validar ---
    record CreateDto(@NotBlank String title) {}

    // --- Controlador de prueba ---
    @RestController
    @RequestMapping("/__errors")
    static class DummyController {
        @PostMapping("/validation")
        void validation(@RequestBody @Valid CreateDto dto) { /* nunca llega */ }

        @GetMapping("/not-found")
        void notFound() { throw new ResourceNotFoundException("ProcessingRequest not found"); }

        @GetMapping("/infra")
        void infra() { throw new DataAccessResourceFailureException("PG down"); }

        @GetMapping("/boom")
        void boom() { throw new RuntimeException("kaboom"); }
    }

    @BeforeEach
    void setUp() {
        this.mvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new RestExceptionHandler()) // <— registra tu advice
                .build();
    }

    @Test
    void mapsValidation_to400_withFields() throws Exception {
        mvc.perform(post("/__errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields[0].field").value("title"));
    }

    @Test
    void mapsNotFound_to404() throws Exception {
        mvc.perform(get("/__errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void mapsInfra_to503() throws Exception {
        mvc.perform(get("/__errors/infra"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void mapsUnexpected_to500() throws Exception {
        mvc.perform(get("/__errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED_ERROR"));
    }
}
