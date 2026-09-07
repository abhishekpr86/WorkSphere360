package com.workflow360.identity.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow360.common.api.PageResponse;
import com.workflow360.common.exception.ResourceAlreadyExistsException;
import com.workflow360.common.exception.ResourceNotFoundException;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;
import com.workflow360.identity.infrastructure.persistence.repository.UserRepository;



@Service
@Transactional(readOnly = true)
public class UserService {
	
	
	public static final int MAX_PAGE_SIZE=100;
	
	private static final Map<String,String> ALLOWED_SORT_FIELDS = Map.of(
			"createdAt","createdAt",
			"updatedAt","updatedAt",
			"email","email",
			"displayName","displayName",
			"status","status");
	
	
	private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc",
			"desc");
	
	
	@Autowired
	private final UserRepository userRepository;
	@Autowired
	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, UserMapper userMapper) {
		super();
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}
	
	
	@Transactional
	public UserResponse createUser(CreateUserRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		
		String normalizeDisplayName = request.displayName().trim();
		
		if(userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResourceAlreadyExistsException("A user with email " + normalizedEmail +"already exists");
		}
		 UserEntity user = new UserEntity(
	                UUID.randomUUID(),
	                normalizedEmail,
	                normalizeDisplayName,
	                UserStatus.PENDING
	        );
		
		 
		 UserEntity savedUser =userRepository.save(user);
		 
		 return userMapper.toResponse(savedUser);
		
		
	}
	
	public UserResponse getUser(UUID id) {
		
		UserEntity user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User Not found with the user id" + id));
		return userMapper.toResponse(user);
	}
	
	
	public List<UserResponse> getUsers(){
		
		
		return userRepository.findAllByOrderByCreatedAtDesc().stream().map(userMapper::toResponse).toList();
	}
	
	
	public PageResponse<UserResponse> searchUsers(){
		return null;
	}
	
	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
		
	
	

}
