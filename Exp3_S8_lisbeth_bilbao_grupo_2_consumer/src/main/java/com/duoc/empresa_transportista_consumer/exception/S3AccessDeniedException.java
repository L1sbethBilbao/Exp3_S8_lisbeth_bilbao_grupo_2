package com.duoc.empresa_transportista_consumer.exception;

public class S3AccessDeniedException extends RuntimeException {

	public S3AccessDeniedException(String operation, Throwable cause) {
		super("Acceso denegado al " + operation, cause);
	}
}
