package com.workflow360.identity.application;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.workflow360.common.api.PageResponse;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

@Component
public class UserMapper {
	
	public UserResponse toResponse(UserEntity entity) {
		return new UserResponse ( entity.getId(),entity.getEmail(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.isEmailVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
	}
	
	
	public PageResponse<UserResponse> toPageResource(Page<UserEntity> page){
		return new PageResponse<>(page.getContent()
				.stream()
				.map(this::toResponse)
				.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
		
		
		
	}

}
