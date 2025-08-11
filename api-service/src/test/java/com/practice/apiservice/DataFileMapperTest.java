package com.practice.apiservice;

import com.practice.apiservice.dto.file.FileUploadResponse;
import com.practice.apiservice.mapper.DataFileMapper;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.user.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataFileMapperTest {
    DataFileMapper mapper = Mappers.getMapper(DataFileMapper.class);

    @Test
    void maps_domain_to_response() {
        var df = new DataFile(UUID.randomUUID(), "ventas.csv", "/x/ventas.csv", 10, null, Instant.now(), User.ofId(UUID.randomUUID()));
        FileUploadResponse dto = mapper.toResponse(df);
        assertThat(dto.id()).isEqualTo(df.id());
        assertThat(dto.originalFilename()).isEqualTo("ventas.csv");
    }
}
