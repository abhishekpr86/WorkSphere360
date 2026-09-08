package com.workflow360.identity.application;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow360.common.api.PageResponse;
import com.workflow360.common.exception.InvalidRequestParameterException;
import com.workflow360.common.exception.OptimisticConflictException;
import com.workflow360.common.exception.ResourceAlreadyExistsException;
import com.workflow360.common.exception.ResourceNotFoundException;
import com.workflow360.identity.api.dto.ChangeUserStatusRequest;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UpdateUserRequest;
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
		
		return userMapper.toResponse(findUser(id));
	}
	
	

	
	
	public PageResponse<UserResponse> searchUsers(String query,UserStatus status,int page,int size,String sortBy,String direction){
		
		
		ValidatePageRequest(page, size, sortBy, direction);
		
		String normalizeQuery  = normalizeQuery(query);
		String entitySortField = ALLOWED_SORT_FIELDS.get(sortBy);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, entitySortField)
        );
        
        
        Page<UserEntity> users = userRepository.search(normalizeQuery, status, pageable);
        
        return userMapper.toPageResource(users);
		
	}
	
	
	@Transactional
	public UserResponse updateUser(UUID id, UpdateUserRequest request) {
		
		UserEntity user = findUser(id);
		verifyVersion(user, request.version());
		user.updateDisplayName(request.displayName().trim());
		return userMapper.toResponse(userRepository.save(user));
	}
	
	@Transactional
	public UserResponse changeStatus(UUID id, ChangeUserStatusRequest request) {
		UserEntity user = findUser(id);
		verifyVersion(user,request.version());
		
		try {
			user.changeStatus(request.status());
		}catch(IllegalStateException e ) {
			throw new InvalidRequestParameterException(e.getMessage());
		}
		return userMapper.toResponse(userRepository.save(user));
	}
	
	private UserEntity findUser(UUID id) {
		return userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found with ID " + id));
	}
	
	public void verifyVersion(UserEntity user,long requestVersion) {
		
		if (user.getVersion() != requestVersion) {
			throw new OptimisticConflictException(" The user has changed since it was loaded."
					                  + "Reload the user and try again.");
		}
		
	}
	
	
	public void ValidatePageRequest(int page,int size,String sortBy,String direction) {
		if (page < 0) {
            throw new InvalidRequestParameterException(
                    "Page number must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestParameterException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }

        if (!ALLOWED_SORT_FIELDS.containsKey(sortBy)) {
            throw new InvalidRequestParameterException(
                    "Unsupported sort field: " + sortBy
            );
        }

        if (!ALLOWED_DIRECTIONS.contains(
                direction.toLowerCase(Locale.ROOT))) {
            throw new InvalidRequestParameterException(
                    "Sort direction must be asc or desc"
            );
        }
		
	}
	
	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
	
	private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        return query.trim();
    }
		
	
	

}
