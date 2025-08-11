package com.practice.apiservice;

import com.practice.apiservice.exception.RestExceptionHandler;
import com.practice.apiservice.repository.irepository.ProcessingStatusFinder;
import com.practice.apiservice.rest.ProcessingQueryController;
import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.ExecutionStatus;
import com.practice.domain.utils.enums.ReaderType;
import com.practice.domain.utils.enums.WriterType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProcessingQueryController.class)
@Import(RestExceptionHandler.class)
class ProcessingQueryControllerTest {

    @Autowired MockMvc mvc;
    @MockBean
    ProcessingStatusFinder finder;

    private ProcessingRequest newRequest(UUID id) {
        var df = DataFile.createForUpload("ventas.csv", "/data/in/ventas.csv", 100, null, UUID.randomUUID());
        var user = User.ofId(UUID.randomUUID());
        var cfg  = BatchJobConfig.builder("etl-ventas")
                .readerType(ReaderType.CSV).writerType(WriterType.NO_OP).build();

        return new ProcessingRequest(
                id, "ETL Ventas Julio", df, Map.of("delimiter",";"),
                user, cfg, Instant.parse("2025-08-07T03:10:21Z"));
    }

    @Test
    void returns200_withoutExecution() throws Exception {
        var id = UUID.randomUUID();
        var pr = newRequest(id);

        Mockito.when(finder.findRequest(id)).thenReturn(Optional.of(pr));
        Mockito.when(finder.findLastExecution(id)).thenReturn(Optional.empty());

        mvc.perform(get("/processings/{id}", id).accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.title", is("ETL Ventas Julio")))
                .andExpect(jsonPath("$.metrics.readCount", is(0)))
                .andExpect(jsonPath("$.lastExecution").doesNotExist());
    }

    @Test
    void returns200_withExecution() throws Exception {
        var id = UUID.randomUUID();
        var pr = newRequest(id);
        var start = Instant.parse("2025-08-07T03:10:22Z");
        var end   = Instant.parse("2025-08-07T03:12:00Z");

        var je = new JobExecution(UUID.randomUUID(), pr, start);
        je.finish(ExecutionStatus.SUCCESS, end, 12345, 12280, 145, null);

        Mockito.when(finder.findRequest(id)).thenReturn(Optional.of(pr));
        Mockito.when(finder.findLastExecution(id)).thenReturn(Optional.of(je));

        mvc.perform(get("/processings/{id}", id).accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(pr.status().name())))
                .andExpect(jsonPath("$.metrics.writeCount", is(12280)))
                .andExpect(jsonPath("$.lastExecution.startTime", is(start.toString())))
                .andExpect(jsonPath("$.lastExecution.endTime", is(end.toString())))
                .andExpect(jsonPath("$.lastExecution.exitStatus", is("SUCCESS")));
    }

    @Test
    void returns404_whenNotFound() throws Exception {
        var id = UUID.randomUUID();
        Mockito.when(finder.findRequest(id)).thenReturn(Optional.empty());

        mvc.perform(get("/processings/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }
}

