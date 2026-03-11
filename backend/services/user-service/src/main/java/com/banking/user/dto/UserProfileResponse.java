package com.banking.user.dto;

import com.banking.user.entity.Address;
import com.banking.user.entity.KycStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private UUID authUserId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationalId;
    private String nationality;
    private Address address;
    private KycStatus kycStatus;
    private LocalDateTime createdAt;
}
