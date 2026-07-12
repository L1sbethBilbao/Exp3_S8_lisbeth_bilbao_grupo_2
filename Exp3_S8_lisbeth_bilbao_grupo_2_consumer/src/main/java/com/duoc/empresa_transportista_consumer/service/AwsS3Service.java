package com.duoc.empresa_transportista_consumer.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_consumer.exception.InvalidFileException;
import com.duoc.empresa_transportista_consumer.exception.S3AccessDeniedException;
import com.duoc.empresa_transportista_consumer.exception.S3BucketNotFoundException;
import com.duoc.empresa_transportista_consumer.exception.S3ObjectNotFoundException;
import com.duoc.empresa_transportista_consumer.exception.S3OperationException;
import com.duoc.empresa_transportista_consumer.exception.S3UploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

	private final S3Client s3Client;

	public void uploadBytes(String bucket, String key, byte[] content, String contentType) {
		if (content == null || content.length == 0) {
			throw new InvalidFileException("El contenido del archivo esta vacio");
		}
		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(contentType != null ? contentType : "application/pdf")
					.contentLength((long) content.length)
					.build();
			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
			log.info("Archivo subido a S3: {}", key);
		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("subir archivo al bucket: " + bucket, e);
			}
			throw new S3UploadException("Error al subir el archivo a S3: " + e.getMessage(), e);
		}
	}

	public void uploadFromBytes(String bucket, String key, byte[] content, String contentType) throws IOException {
		uploadBytes(bucket, key, content, contentType);
	}

	public void moveObject(String bucket, String sourceKey, String destKey) {
		try {
			CopyObjectRequest copyRequest = CopyObjectRequest.builder()
					.sourceBucket(bucket)
					.sourceKey(sourceKey)
					.destinationBucket(bucket)
					.destinationKey(destKey)
					.build();
			s3Client.copyObject(copyRequest);
			deleteObject(bucket, sourceKey);
			log.info("Objeto movido de {} a {}", sourceKey, destKey);
		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(sourceKey, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("mover objeto en el bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al mover el objeto", e);
		}
	}

	public void deleteObject(String bucket, String key) {
		try {
			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
			s3Client.deleteObject(deleteRequest);
			log.info("Objeto eliminado de S3: {}", key);
		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("eliminar objeto del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al eliminar el objeto: " + key, e);
		}
	}
}
