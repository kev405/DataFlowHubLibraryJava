package com.practice.apiservice;

import com.practice.apiservice.entity.*;
import com.practice.apiservice.repository.*;
import com.practice.domain.utils.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class RepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    // Fallback si tu Boot < 3.1 (no molesta si es 3.1+)
    @DynamicPropertySource
    static void pgProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired UserRepository users;
    @Autowired DataFileRepository files;
    @Autowired BatchJobConfigRepository cfgs;
    @Autowired ProcessingRequestRepository reqs;
    @Autowired JobExecutionRepository execs;

    @Test
    void persistsProcessingRequest_withJsonb_andFindsLastExecution() {
        // ---------- seed: user ----------
        var user = new UserEntity(UUID.randomUUID(), "tester", "tester@gmail.com", UserRole.ADMIN, Instant.now());
        users.save(user);

        // ---------- seed: data file ----------
        var df = new DataFileEntity();
        df.setId(UUID.randomUUID());
        df.setOriginalFilename("ventas.csv");
        df.setStoragePath("/data/in/ventas.csv");
        df.setSizeBytes(1000);
        df.setChecksumSha256(null);
        df.setUploadedAt(Instant.parse("2025-08-07T10:00:00Z"));
        df.setUploadedBy(user);
        files.save(df);

        // ---------- seed: batch job config ----------
        var cfg = new BatchJobConfigEntity();
        cfg.setId(UUID.randomUUID());
        cfg.setName("etl-ventas");
        cfg.setDescription("carga de ventas");
        cfg.setChunkSize(1000);
        cfg.setReaderType(ReaderType.CSV);
        cfg.setWriterType(WriterType.NO_OP);
        cfg.setAllowRestart(false);
        cfg.setCreatedAt(Instant.parse("2025-08-07T09:59:00Z"));
        cfg.setActive(true);
        cfgs.save(cfg);

        // ---------- seed: processing request (con parameters como JSONB) ----------
        var pr = new ProcessingRequestEntity();
        var prId = UUID.randomUUID();
        pr.setId(prId);
        pr.setTitle("ETL Ventas Julio");
        pr.setDataFile(df);
        pr.setParameters(Map.of("delimiter", ";", "trim", "true"));
        pr.setStatus(RequestStatus.IN_PROGRESS);
        pr.setCreatedAt(Instant.parse("2025-08-07T10:01:00Z"));
        pr.setRequestedBy(user);
        pr.setBatchJobConfig(cfg);
        reqs.save(pr);

        // ---------- seed: 3 job executions con distintos startTime ----------
        var t1 = Instant.parse("2025-08-07T10:02:00Z");
        var t2 = Instant.parse("2025-08-07T10:05:00Z"); // <- la última (esperada)
        var t3 = Instant.parse("2025-08-07T10:03:00Z");

        var e1 = new JobExecutionEntity();
        e1.setId(UUID.randomUUID());
        e1.setProcessingRequest(pr);
        e1.setStartTime(t1);
        e1.setEndTime(Instant.parse("2025-08-07T10:02:30Z"));
        e1.setExitStatus(ExecutionStatus.SUCCESS);
        e1.setReadCount(100); e1.setWriteCount(95); e1.setSkipCount(5);
        execs.save(e1);

        var e2 = new JobExecutionEntity();
        e2.setId(UUID.randomUUID());
        e2.setProcessingRequest(pr);
        e2.setStartTime(t2);
        e2.setEndTime(null);
        e2.setExitStatus(null);
        e2.setReadCount(1200); e2.setWriteCount(1180); e2.setSkipCount(20);
        execs.save(e2);

        var e3 = new JobExecutionEntity();
        e3.setId(UUID.randomUUID());
        e3.setProcessingRequest(pr);
        e3.setStartTime(t3);
        e3.setEndTime(Instant.parse("2025-08-07T10:04:10Z"));
        e3.setExitStatus(ExecutionStatus.FAIL);
        e3.setReadCount(500); e3.setWriteCount(450); e3.setSkipCount(50);
        e3.setErrorMessage("mock fail");
        execs.save(e3);

        // ---------- verificación: última ejecución por startTime ----------
        var last = execs.findTop1ByProcessingRequestIdOrderByStartTimeDesc(prId)
                .orElseThrow();
        assertThat(last.getStartTime()).isEqualTo(t2)      // exact match
                .isAfter(t1).isAfter(t3); // > los otros

        // ---------- verificación: JSONB parameters ----------
        var reloaded = reqs.findById(prId).orElseThrow();
        assertThat(reloaded.getParameters())
                .containsEntry("delimiter", ";")
                .containsEntry("trim", "true");

        // ---------- verificación: query por status (paginada) ----------
        var page = reqs.findByStatus(RequestStatus.IN_PROGRESS,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(ProcessingRequestEntity::getId)
                .contains(prId);
    }
}
