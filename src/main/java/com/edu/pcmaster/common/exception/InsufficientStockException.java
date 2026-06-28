package com.edu.pcmaster.common.exception;

public class InsufficientStockException extends BadRequestException {
	public InsufficientStockException(String message) {
		super(message);
	}
}
