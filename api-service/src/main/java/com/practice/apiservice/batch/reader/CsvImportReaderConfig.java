package com.practice.apiservice.batch.reader;

import com.practice.apiservice.model.ImportRecord;
import java.nio.charset.StandardCharsets;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class CsvImportReaderConfig {

    private static final String EXPECTED_HEADER = "external_id,user_email,amount,event_time";

    @Bean
    @StepScope
    public FlatFileItemReader<ImportRecord> importRecordReader(
            @Value("#{jobParameters['storagePath']}") String storagePath,
            @Value("#{jobParameters['delimiter']?:','}") String delimiter) {

        FlatFileItemReader<ImportRecord> reader = new FlatFileItemReader<>();
        reader.setName("importRecordReader");
        reader.setEncoding(StandardCharsets.UTF_8.name());
        reader.setResource(new FileSystemResource(storagePath));
        reader.setSaveState(true);

        // Falla si el archivo no existe (puedes poner false si prefieres validar antes)
        reader.setStrict(true);

        // Header
        reader.setLinesToSkip(1);
        reader.setSkippedLinesCallback(line -> {
            if (!EXPECTED_HEADER.equals(line)) {
                throw new FlatFileParseException(
                        "Invalid header. Expected: " + EXPECTED_HEADER + " but was: " + line, line, 1);
            }
        });

        // Tokenizer + mapper
        reader.setLineMapper(lineMapper(delimiter));
        return reader;
    }

    private LineMapper<ImportRecord> lineMapper(String delimiter) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(delimiter);
        tokenizer.setQuoteCharacter('"');            // admite comillas
        tokenizer.setStrict(true);
        tokenizer.setNames("external_id", "user_email", "amount", "event_time");

        DefaultLineMapper<ImportRecord> mapper = new DefaultLineMapper<>();
        mapper.setLineTokenizer(tokenizer);
        mapper.setFieldSetMapper(new ImportRecordFieldSetMapper());
        mapper.afterPropertiesSet();
        return mapper;
    }
}
