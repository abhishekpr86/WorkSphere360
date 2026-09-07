package com.workflow360.common.exception;

public record FieldValidationError (
		String field,
        String message){

}
