package com.workflow360.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest( @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @NotBlank(message = "Display name is required")
        @Size(
            min = 2,
            max = 150,
            message = "Display name must contain between 2 and 150 characters"
        )
        String displayName) {

}
