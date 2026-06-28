package com.edu.pcmaster.common.exception;

public class InvalidCredentialsException extends BadRequestException {
	public InvalidCredentialsException(String message) {
		super(message);
	}
}
