package com.duoc.empresa_transportista_consumer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_consumer.exception.InvalidFileException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	public void saveBytes(String filename, byte[] content) throws IOException {
		Path dest = resolveFilePath(filename);
		Files.createDirectories(dest.getParent());
		Files.write(dest, content);
		log.info("Archivo guardado en EFS: {}", dest.toAbsolutePath());
	}

	public byte[] readBytes(String relativePath) throws IOException {
		Path path = resolveFilePath(relativePath);
		if (!Files.exists(path)) {
			throw new InvalidFileException("Archivo staging no encontrado: " + relativePath);
		}
		return Files.readAllBytes(path);
	}

	public void deleteFile(String filename) throws IOException {
		Path efsRoot = Paths.get(efsPath).normalize().toAbsolutePath();
		Path candidate = resolveFilePath(filename);
		if (Files.exists(candidate)) {
			Files.delete(candidate);
			log.info("Archivo eliminado de EFS: {}", candidate);
			deleteEmptyParents(candidate.getParent(), efsRoot);
		}
	}

	public void deleteStagingDirectory(String stagingPath) throws IOException {
		Path stagingDir = resolveFilePath(stagingPath).getParent();
		if (stagingDir == null || !Files.exists(stagingDir)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(stagingDir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					log.warn("No se pudo eliminar {}: {}", path, e.getMessage());
				}
			});
		}
		log.info("Staging eliminado: {}", stagingDir);
	}

	private Path resolveFilePath(String filename) {
		String normalized = filename.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		return Paths.get(efsPath, normalized.split("/")).normalize();
	}

	private void deleteEmptyParents(Path directory, Path efsRoot) throws IOException {
		Path current = directory;
		while (current != null && current.startsWith(efsRoot) && !current.equals(efsRoot)) {
			if (!Files.isDirectory(current) || !isDirectoryEmpty(current)) {
				break;
			}
			Files.delete(current);
			current = current.getParent();
		}
	}

	private boolean isDirectoryEmpty(Path directory) throws IOException {
		try (var entries = Files.list(directory)) {
			return entries.findAny().isEmpty();
		}
	}
}
