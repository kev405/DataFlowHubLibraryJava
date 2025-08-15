package com.practice.apiservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.practice.apiservice.batch.reader.CsvImportReaderConfig;
import com.practice.apiservice.model.ImportRecord;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.batch.test.StepScopeTestUtils;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBatchTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CsvImportReaderTest.TestConfig.class,
        CsvImportReaderConfig.class
})
class CsvImportReaderTest {

    @Configuration
    @ComponentScan(basePackages = "com.practice.apiservice.batch.reader")
    static class TestConfig {}

    @SuppressWarnings("unchecked")
    @Test
    void reads_csv_with_params_injected_at_stepscope() throws Exception {
        // 1) Crear CSV temporal
        Path file = Files.createTempFile("import_" + UUID.randomUUID(), ".csv");
        String csv = String.join("\n",
                "external_id,user_email,amount,event_time",
                "A1,a@x.com,12.34,2025-08-12T00:15:00Z",
                "A2,b@x.com,99.00,2025-08-13T10:00:00Z"
        );
        Files.writeString(file, csv, StandardCharsets.UTF_8);

        // 2) JobParameters
        JobParameters params = new JobParametersBuilder()
                .addString("storagePath", file.toString())
                .addString("delimiter", ",")
                .toJobParameters();

        // 3) Obtener bean StepScope y leer bajo contexto de step
        FlatFileItemReader<ImportRecord> reader =
                new CsvImportReaderConfig().importRecordReader(file.toString(), ",");

        StepScopeTestUtils.doInStepScope(
                MetaDataInstanceFactory.createStepExecution(params),
                () -> {
                    reader.open(new ExecutionContext());
                    ImportRecord r1 = reader.read();
                    ImportRecord r2 = reader.read();
                    ImportRecord r3 = reader.read();
                    reader.close();

                    assertThat(r1.getExternalId()).isEqualTo("A1");
                    assertThat(r2.getUserEmail()).isEqualTo("b@x.com");
                    assertThat(r3).isNull(); // EOF
                    return null;
                }
        );
    }
}
