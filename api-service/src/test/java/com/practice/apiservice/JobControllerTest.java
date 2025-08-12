package com.practice.apiservice;

import com.practice.apiservice.config.SecurityConfig;
import com.practice.apiservice.dto.batch.RunJobRequest;
import com.practice.apiservice.metrics.JobMetrics;
import com.practice.apiservice.rest.JobController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired MockMvc mvc;

    @MockBean JobLauncher jobLauncher;
    @MockBean JobRegistry jobRegistry;
    @MockBean JobMetrics jobMetrics;

    @Test
    void run_returns202_and_location() throws Exception {
        Job mockJob = Mockito.mock(Job.class);
        when(jobRegistry.getJob("csv_to_ipa_v1")).thenReturn(mockJob);

        JobExecution exec = new JobExecution(456L);
        JobInstance ji = new JobInstance(123L, "csv_to_ipa_v1");
        exec.setJobInstance(ji);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(exec);

        String json = """
      {
        "processingRequestId": "7e2a1d7c-3b9b-4f1a-8a55-9a2f1e4c7788",
        "parameters": {"delimiter":";"}
      }
      """;

        mvc.perform(post("/jobs/csv_to_ipa_v1/run")
                        .with(httpBasic("admin","admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location",
                        "/processings/7e2a1d7c-3b9b-4f1a-8a55-9a2f1e4c7788"))
                .andExpect(jsonPath("$.jobInstanceId").value(123))
                .andExpect(jsonPath("$.jobExecutionId").value(456));
    }

    @Test
    void run_requires_admin_403_for_user() throws Exception {
        when(jobRegistry.getJob("csv_to_ipa_v1")).thenReturn(Mockito.mock(Job.class));

        String json = """
      { "processingRequestId": "7e2a1d7c-3b9b-4f1a-8a55-9a2f1e4c7788" }
      """;

        mvc.perform(post("/jobs/csv_to_ipa_v1/run")
                        .with(httpBasic("user","user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
