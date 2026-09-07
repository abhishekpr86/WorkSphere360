package com.workflow360.common.exception;

public class ResourceAlreadyExistsException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;
	
	
	public ResourceAlreadyExistsException(String message) {
		super(message);
	}

}
