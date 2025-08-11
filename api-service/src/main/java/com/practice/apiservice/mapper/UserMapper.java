package com.practice.apiservice.mapper;

import com.practice.apiservice.entity.UserEntity;
import com.practice.domain.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default User toDomain(UserEntity e) { return User.ofId(e.getId()); }
    default UserEntity toEntity(User u) { return new UserEntity(u.id(), u.name(), u.email(), u.role(), u.createdAt()); }
}
