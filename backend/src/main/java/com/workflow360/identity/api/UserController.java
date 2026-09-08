package com.workflow360.identity.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow360.common.api.PageResponse;
import com.workflow360.identity.api.dto.ChangeUserStatusRequest;
import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UpdateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.application.UserService;
import com.workflow360.identity.domain.UserStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
	
	
	private final UserService userService;

	public UserController(UserService userService) {
		super();
		this.userService = userService;
	}
	
	
	
	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
		
		UserResponse response = userService.createUser(request);
		URI location = URI.create("/api/v1/users/" + response.id() );
		
		return ResponseEntity.created(location).body(response);
		
	
		
		
		
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable UUID id){
		return ResponseEntity.ok(userService.getUser(id));
	}
	
	@GetMapping
	public ResponseEntity<PageResponse<UserResponse>> searchUsers(      
			@RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction){
		
		
		
		return ResponseEntity.ok(userService.searchUsers(query, status, page, size, sortBy, direction));
		
		
		
		
	}
	
	
	@PostMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request){
		
		return ResponseEntity.ok(userService.updateUser(id, request));
		
	}
	
	
	@PostMapping("/{id}/status")
	public ResponseEntity<UserResponse> ChangeStatus(@PathVariable UUID id,@Valid @RequestBody ChangeUserStatusRequest request){
		
		return ResponseEntity.ok(userService.changeStatus(id, request));
		
	}
	
	
	
	

}
