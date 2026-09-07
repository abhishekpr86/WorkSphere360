package com.workflow360.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		
		
		@NotBlank(message="Display name is required")
		@Size(min = 2,max = 150,message = "Display name must contain  the min 2 to max 150 charaters")
		String displayName, 
		@PositiveOrZero(message ="Version Must be zero or greater")
		long version){
	

}
