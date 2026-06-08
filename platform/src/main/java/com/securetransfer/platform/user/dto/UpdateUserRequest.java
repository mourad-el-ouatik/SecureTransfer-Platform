// UpdateUserRequest.java
package com.securetransfer.platform.user.dto;

public record UpdateUserRequest(
        String phoneNumber,
        String nationality
) {}
