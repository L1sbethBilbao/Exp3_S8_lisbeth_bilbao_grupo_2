package com.duoc.empresa_transportista_consumer.service;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_consumer.exception.InvalidFileException;

@Service
public class GuiaDespachoService {

	public String buildKey(String fecha, String transportista, String nombreGuia) {
		return fecha.trim() + "/" + transportista.trim() + "/" + normalizarNombreGuia(nombreGuia);
	}

	public String buildActualizadoKey(String fecha, String transportista, String nombreGuia) {
		validarParametros(fecha, transportista, nombreGuia);
		String nombreActualizado = normalizarNombreGuia(nombreGuia).replace(".pdf", "") + "-actualizado";
		return buildKey(fecha, transportista, nombreActualizado);
	}

	public String obtenerNombreArchivo(String key) {
		int lastSlash = key.lastIndexOf('/');
		return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
	}

	private String normalizarNombreGuia(String nombreGuia) {
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
		String nombre = nombreGuia.trim();
		if (!nombre.toLowerCase().endsWith(".pdf")) {
			nombre = nombre + ".pdf";
		}
		return nombre;
	}

	private void validarParametros(String fecha, String transportista, String nombreGuia) {
		if (fecha == null || fecha.isBlank()) {
			throw new InvalidFileException("La fecha es obligatoria");
		}
		if (transportista == null || transportista.isBlank()) {
			throw new InvalidFileException("El transportista es obligatorio");
		}
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
	}
}
