package com.practice.apiservice;

import com.practice.apiservice.metrics.JobMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("metricstest")
class MetricsActuatorIT {

    @Autowired MockMvc mvc;
    @Autowired JobMetrics jobMetrics;

    @Test
    void counter_is_exposed_and_increments() throws Exception {
        // incrementamos diferentes tags
        jobMetrics.incrementJobExecutions("success");
        jobMetrics.incrementJobExecutions("fail");

        mvc.perform(get("/actuator/metrics/" + JobMetrics.JOB_EXEC_COUNTER)
                        .with(httpBasic("user","user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(JobMetrics.JOB_EXEC_COUNTER))
                .andExpect(jsonPath("$.measurements[0].value").exists());
    }
}
