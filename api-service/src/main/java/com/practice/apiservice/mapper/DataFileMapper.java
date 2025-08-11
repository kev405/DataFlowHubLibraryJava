package com.practice.apiservice.mapper;

import org.mapstruct.Mapper;
import com.practice.apiservice.entity.DataFileEntity;
import com.practice.domain.user.User;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface DataFileMapper {
    default com.practice.domain.datafile.DataFile toDomain(DataFileEntity e) {
        return new com.practice.domain.datafile.DataFile(
                e.getId(), e.getOriginalFilename(), e.getStoragePath(),
                e.getSizeBytes(), e.getChecksumSha256(), e.getUploadedAt(),
                User.ofId(e.getUploadedBy().getId()));
    }
}
