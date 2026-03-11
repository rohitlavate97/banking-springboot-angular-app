package com.banking.user.service;

import com.banking.user.dto.UpdateProfileRequest;
import com.banking.user.dto.UserProfileResponse;
import com.banking.user.entity.KycStatus;
import com.banking.user.entity.UserProfile;
import com.banking.user.exception.UserNotFoundException;
import com.banking.user.mapper.UserProfileMapper;
import com.banking.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional
    public UserProfileResponse createProfile(UUID authUserId, String email) {
        if (userProfileRepository.findByAuthUserId(authUserId).isPresent()) {
            return getProfileByAuthUserId(authUserId);
        }
        UserProfile profile = UserProfile.builder()
                .authUserId(authUserId)
                .email(email)
                .firstName("")
                .lastName("")
                .kycStatus(KycStatus.PENDING)
                .build();
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Created user profile for authUserId: {}", authUserId);
        return userProfileMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for email: " + email));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfileByEmail(String email, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for email: " + email));
        userProfileMapper.updateFromRequest(request, profile);
        UserProfile updated = userProfileRepository.save(profile);
        log.info("Updated profile for email: {}", email);
        return userProfileMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByAuthUserId(UUID authUserId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user: " + authUserId));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Profile not found with id: " + id));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID authUserId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user: " + authUserId));

        userProfileMapper.updateFromRequest(request, profile);
        UserProfile updated = userProfileRepository.save(profile);
        log.info("Updated profile for authUserId: {}", authUserId);
        return userProfileMapper.toResponse(updated);
    }

    @Transactional
    public UserProfileResponse updateKycStatus(UUID profileId, KycStatus status) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found with id: " + profileId));

        profile.setKycStatus(status);
        if (status == KycStatus.VERIFIED) {
            profile.setKycVerifiedAt(LocalDateTime.now());
        }
        UserProfile updated = userProfileRepository.save(profile);
        log.info("KYC status updated to {} for profile: {}", status, profileId);
        return userProfileMapper.toResponse(updated);
    }
}
