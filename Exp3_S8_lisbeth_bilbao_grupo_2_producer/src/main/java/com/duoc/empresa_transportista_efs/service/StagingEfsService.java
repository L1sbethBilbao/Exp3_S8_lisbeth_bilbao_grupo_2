package com.duoc.empresa_transportista_efs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.exception.InvalidFileException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StagingEfsService {

	@Value("${efs.path}")
	private String efsPath;

	public String guardarEnStaging(String messageId, MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("El archivo esta vacio o es nulo");
		}

		String originalName = file.getOriginalFilename();
		if (originalName == null || originalName.isBlank()) {
			originalName = "archivo.pdf";
		}

		Path stagingDir = Paths.get(efsPath, "staging", messageId);
		Files.createDirectories(stagingDir);
		Path dest = stagingDir.resolve(originalName.trim());
		file.transferTo(dest);
		String relativePath = "staging/" + messageId + "/" + originalName.trim();
		log.info("Archivo guardado en staging EFS: {}", relativePath);
		return relativePath;
	}
}
