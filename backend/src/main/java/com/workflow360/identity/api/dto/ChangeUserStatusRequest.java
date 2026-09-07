package com.workflow360.identity.api.dto;

import com.workflow360.identity.domain.UserStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeUserStatusRequest(
		
		@NotNull(message = "User status is required")
		UserStatus status,
		
		@PositiveOrZero(message = "The version must either zero or greater")
		long version
		
		) {
	

}
