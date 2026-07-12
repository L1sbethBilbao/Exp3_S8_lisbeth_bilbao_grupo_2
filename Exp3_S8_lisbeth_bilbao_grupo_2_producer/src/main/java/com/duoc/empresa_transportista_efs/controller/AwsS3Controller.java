package com.duoc.empresa_transportista_efs.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaConsultaResponse;
import com.duoc.empresa_transportista_efs.dto.OperacionEncoladaResponse;
import com.duoc.empresa_transportista_efs.dto.S3ObjectDto;
import com.duoc.empresa_transportista_efs.service.AwsS3Service;
import com.duoc.empresa_transportista_efs.service.GuiaDespachoService;
import com.duoc.empresa_transportista_efs.service.GuiaS3ProductorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class AwsS3Controller {

	private final AwsS3Service awsS3Service;
	private final GuiaDespachoService guiaDespachoService;
	private final GuiaS3ProductorService guiaS3ProductorService;

	@GetMapping("/{bucket}/objects")
	public ResponseEntity<List<S3ObjectDto>> listObjects(@PathVariable String bucket) {
		List<S3ObjectDto> dtoList = awsS3Service.listObjects(bucket);
		return ResponseEntity.ok(dtoList);
	}

	@GetMapping("/{bucket}/consulta")
	public ResponseEntity<GuiaConsultaResponse> consultarGuias(@PathVariable String bucket,
			@RequestParam String fecha, @RequestParam(required = false) String transportista) {
		GuiaConsultaResponse response = guiaDespachoService.consultarGuias(bucket, fecha, transportista);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{bucket}/object")
	public ResponseEntity<byte[]> downloadObject(@PathVariable String bucket,
			@RequestParam(required = false) String key, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista, @RequestParam(required = false) String nombreGuia) {

		String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
		byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, resolvedKey);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resolvedKey + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileBytes);
	}

	@PostMapping("/{bucket}/object")
	public ResponseEntity<OperacionEncoladaResponse> uploadObject(@PathVariable String bucket,
			@RequestParam(required = false) String key, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista, @RequestParam(required = false) String nombreGuia,
			@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "X-Simular-Error", defaultValue = "false") String simularError) {

		try {
			OperacionEncoladaResponse response = guiaS3ProductorService.encolarSubida(bucket, key, fecha,
					transportista, nombreGuia, file, parseSimularError(simularError));
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
		} catch (Exception e) {
			log.error("Error al encolar subida de guia: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PutMapping("/{bucket}/object")
	public ResponseEntity<OperacionEncoladaResponse> updateObject(@PathVariable String bucket,
			@RequestParam(required = false) String key, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista, @RequestParam(required = false) String nombreGuia,
			@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "X-Simular-Error", defaultValue = "false") String simularError) {

		try {
			OperacionEncoladaResponse response = guiaS3ProductorService.encolarActualizacion(bucket, key, fecha,
					transportista, nombreGuia, file, parseSimularError(simularError));
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
		} catch (Exception e) {
			log.error("Error al encolar actualizacion de guia: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/{bucket}/move")
	public ResponseEntity<OperacionEncoladaResponse> moveObject(@PathVariable String bucket,
			@RequestParam String sourceKey, @RequestParam String destKey,
			@RequestHeader(value = "X-Simular-Error", defaultValue = "false") String simularError) {

		OperacionEncoladaResponse response = guiaS3ProductorService.encolarMovimiento(bucket, sourceKey, destKey,
				parseSimularError(simularError));
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	@DeleteMapping("/{bucket}/object")
	public ResponseEntity<OperacionEncoladaResponse> deleteObject(@PathVariable String bucket,
			@RequestParam(required = false) String key, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista, @RequestParam(required = false) String nombreGuia,
			@RequestHeader(value = "X-Simular-Error", defaultValue = "false") String simularError) {

		try {
			OperacionEncoladaResponse response = guiaS3ProductorService.encolarEliminacion(bucket, key, fecha,
					transportista, nombreGuia, parseSimularError(simularError));
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
		} catch (Exception e) {
			log.error("Error al encolar eliminacion de guia: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	private boolean parseSimularError(String value) {
		return "true".equalsIgnoreCase(value);
	}
}
