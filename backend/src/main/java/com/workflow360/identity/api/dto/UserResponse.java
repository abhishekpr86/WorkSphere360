package com.workflow360.identity.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.workflow360.identity.domain.UserStatus;

public record UserResponse( 
		UUID id,
        String email,
        String displayName,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        long version) {

}
