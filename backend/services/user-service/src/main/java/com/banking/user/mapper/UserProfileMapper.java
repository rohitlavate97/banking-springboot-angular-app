package com.banking.user.mapper;

import com.banking.user.dto.UpdateProfileRequest;
import com.banking.user.dto.UserProfileResponse;
import com.banking.user.entity.UserProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authUserId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "kycStatus", ignore = true)
    @Mapping(target = "kycVerifiedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(UpdateProfileRequest request, @MappingTarget UserProfile entity);
}
