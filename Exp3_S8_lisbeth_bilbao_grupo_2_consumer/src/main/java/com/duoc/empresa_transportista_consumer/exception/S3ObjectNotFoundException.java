package com.duoc.empresa_transportista_consumer.exception;

public class S3ObjectNotFoundException extends RuntimeException {

	public S3ObjectNotFoundException(String key, String bucket, Throwable cause) {
		super("Objeto no encontrado: " + key + " en bucket " + bucket, cause);
	}
}
