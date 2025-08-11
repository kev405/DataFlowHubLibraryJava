package com.practice.apiservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"  // Disable Flyway for tests
})
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void health_is_public_200() throws Exception {
        mvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void processings_requires_auth_401_without_credentials() throws Exception {
        // This will test that any processing endpoint requires authentication
        // Using a UUID format to match the actual controller path
        mvc.perform(get("/processings/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void processings_ok_with_user_credentials() throws Exception {
        // This will return 404 since there's no actual processing, but it shows authentication works
        mvc.perform(get("/processings/123e4567-e89b-12d3-a456-426614174000").with(httpBasic("user","user123")))
                .andExpect(status().isNotFound()); // 404 instead of 401 means auth worked
    }

    @Test 
    void file_upload_requires_auth_401_without_credentials() throws Exception {
        mvc.perform(post("/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void file_upload_ok_with_user_credentials() throws Exception {
        // This will return 400 (bad request) due to missing body, but it shows authentication works
        mvc.perform(post("/files").with(httpBasic("user","user123")))
                .andExpect(status().isBadRequest()); // 400 instead of 401 means auth worked
    }
}
