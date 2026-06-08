// CreateAgenceRequest.java
package com.securetransfer.platform.user.dto;

import jakarta.validation.constraints.*;

public record CreateAgenceRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String phoneNumber,
        String licenseNumber,
        String city,
        String address
) {}
