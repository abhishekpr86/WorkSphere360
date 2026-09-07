package com.workflow360.common.exception;

public class OptimisticConflictException extends RuntimeException {
	
	public final static long serialVersionUID = 1L;
	
	
	public OptimisticConflictException(String message) {
		super(message);
	}

}
