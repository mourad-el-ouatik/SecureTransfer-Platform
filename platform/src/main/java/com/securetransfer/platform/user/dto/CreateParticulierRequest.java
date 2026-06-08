// CreateParticulierRequest.java — dans user/dto/
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateParticulierRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @Pattern(regexp = "\\+?[0-9]{10,15}") String phoneNumber,
        @NotBlank String cin,
        @NotNull LocalDate dateOfBirth,
        String nationality
) {}
