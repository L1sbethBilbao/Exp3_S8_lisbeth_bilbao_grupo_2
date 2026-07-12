package com.duoc.empresa_transportista_consumer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.empresa_transportista_consumer.dto.ColaEstadoResponse;
import com.duoc.empresa_transportista_consumer.dto.DlqMensajesResponse;
import com.duoc.empresa_transportista_consumer.dto.ProcesamientoManualResponse;
import com.duoc.empresa_transportista_consumer.service.ColaMonitoreoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ColaController {

	private final ColaMonitoreoService colaMonitoreoService;

	@GetMapping("/cola/estado")
	public ResponseEntity<ColaEstadoResponse> estadoColaPrincipal() {
		return ResponseEntity.ok(colaMonitoreoService.obtenerEstadoColaPrincipal());
	}

	@GetMapping("/cola/dlq/estado")
	public ResponseEntity<ColaEstadoResponse> estadoDlq() {
		return ResponseEntity.ok(colaMonitoreoService.obtenerEstadoDlq());
	}

	@GetMapping("/dlq/mensajes")
	public ResponseEntity<DlqMensajesResponse> listarMensajesDlq(
			@RequestParam(defaultValue = "10") int limite) {
		return ResponseEntity.ok(colaMonitoreoService.listarMensajesDlq(limite));
	}

	@PostMapping("/cola/procesar-uno")
	public ResponseEntity<ProcesamientoManualResponse> procesarUno() {
		return ResponseEntity.ok(colaMonitoreoService.procesarUnoManual());
	}
}
