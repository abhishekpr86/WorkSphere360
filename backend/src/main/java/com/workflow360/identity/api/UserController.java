package com.workflow360.identity.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow360.identity.api.dto.CreateUserRequest;
import com.workflow360.identity.api.dto.UserResponse;
import com.workflow360.identity.application.UserService;

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
	public ResponseEntity<List<UserResponse>> getUsers(){
		return ResponseEntity.ok(userService.getUsers());
		
	}
	
	
	
	
	
	
	

}
