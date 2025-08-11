package com.practice.apiservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.practice.apiservice.dto.file.FileUploadResponse;
import com.practice.apiservice.entity.DataFileEntity;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.user.User;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface DataFileMapper {
    default com.practice.domain.datafile.DataFile toDomain(DataFileEntity e) {
        return new com.practice.domain.datafile.DataFile(
                e.getId(), e.getOriginalFilename(), e.getStoragePath(),
                e.getSizeBytes(), e.getChecksumSha256(), e.getUploadedAt(),
                User.ofId(e.getUploadedBy().getId()));
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "originalFilename", source = "originalFilename")
    FileUploadResponse toResponse(DataFile df);
}
