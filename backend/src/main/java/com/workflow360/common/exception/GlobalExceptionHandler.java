package com.workflow360.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(InvalidRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRequestParameterException
	(InvalidRequestParameterException exception,
			HttpServletRequest request){
		
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_REQUEST_PARAMETER",
				exception.getMessage(),
				request.getRequestURI(),
				List.of()
				);
		
		
		
		return ResponseEntity.badRequest().body(response);
	}
	
	@ExceptionHandler(OptimisticConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleOptimisticConflict(
	        OptimisticConflictException exception,
	        HttpServletRequest request) {

	    ApiErrorResponse response = new ApiErrorResponse(
	            Instant.now(),
	            HttpStatus.CONFLICT.value(),
	            "STALE_RESOURCE_VERSION",
	            exception.getMessage(),
	            request.getRequestURI(),
	            List.of()
	    );

	    return ResponseEntity
	            .status(HttpStatus.CONFLICT)
	            .body(response);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(
	        ObjectOptimisticLockingFailureException exception,
	        HttpServletRequest request) {

	    ApiErrorResponse response = new ApiErrorResponse(
	            Instant.now(),
	            HttpStatus.CONFLICT.value(),
	            "STALE_RESOURCE_VERSION",
	            "The user was modified by another request. Reload and try again.",
	            request.getRequestURI(),
	            List.of()
	    );

	    return ResponseEntity
	            .status(HttpStatus.CONFLICT)
	            .body(response);
	}
	
	

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception,HttpServletRequest request){
		
		ApiErrorResponse response =new ApiErrorResponse(
				Instant .now(),
		        HttpStatus.NOT_FOUND.value(),
		        "RESOURCE_NOT_FOUND",
		        exception.getMessage(),
		        request.getRequestURI(),
		        List.of()
		      	);
		
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		
	}
	
	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ResourceAlreadyExistsException exception,HttpServletRequest request){
		
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.CONFLICT.value(),
				"RESOURCE_ALREADY_EXIST",
				exception.getMessage(),
				request.getRequestURI(),
				List.of()
				
				
				);
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,HttpServletRequest request){
		
		List<FieldValidationError> filedErrors = 
				exception
				.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toFieldValidationError)
				.toList();
		
		
		
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"VALIDATION_ERROR",
				"Requestion Validation Failed",
				request.getRequestURI(),
				filedErrors);
		
		return null;
		
	}
	
	private FieldValidationError toFieldValidationError(FieldError error) {
		
		return new FieldValidationError(error.getField(),error.getDefaultMessage());
		
	}
}
