package com.banking.user.controller;

import com.banking.user.dto.UpdateProfileRequest;
import com.banking.user.dto.UserProfileResponse;
import com.banking.user.entity.KycStatus;
import com.banking.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user's profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("X-Authenticated-User") String email) {
        return ResponseEntity.ok(userProfileService.getProfileByEmail(email));
    }

    @PutMapping("/me")
    @Operation(summary = "Update authenticated user's profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-Authenticated-User") String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfileByEmail(email, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user profile by ID (Admin only)")
    public ResponseEntity<UserProfileResponse> getProfileById(@PathVariable UUID id) {
        return ResponseEntity.ok(userProfileService.getProfileById(id));
    }

    @PutMapping("/{id}/kyc")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update KYC status (Admin only)")
    public ResponseEntity<UserProfileResponse> updateKyc(
            @PathVariable UUID id,
            @RequestParam KycStatus status) {
        return ResponseEntity.ok(userProfileService.updateKycStatus(id, status));
    }
}
