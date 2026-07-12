package com.duoc.empresa_transportista_consumer.exception;

public class S3BucketNotFoundException extends RuntimeException {

	public S3BucketNotFoundException(String bucket, Throwable cause) {
		super("Bucket no encontrado: " + bucket, cause);
	}
}
