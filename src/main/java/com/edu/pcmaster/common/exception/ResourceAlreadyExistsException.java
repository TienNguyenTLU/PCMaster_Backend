package com.edu.pcmaster.common.exception;

public class ResourceAlreadyExistsException extends BadRequestException {
	public ResourceAlreadyExistsException(String message) {
		super(message);
	}
}
