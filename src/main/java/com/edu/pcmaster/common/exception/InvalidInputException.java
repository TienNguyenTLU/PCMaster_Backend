package com.edu.pcmaster.common.exception;

public class InvalidInputException extends BadRequestException {
	public InvalidInputException(String message) {
		super(message);
	}
}
