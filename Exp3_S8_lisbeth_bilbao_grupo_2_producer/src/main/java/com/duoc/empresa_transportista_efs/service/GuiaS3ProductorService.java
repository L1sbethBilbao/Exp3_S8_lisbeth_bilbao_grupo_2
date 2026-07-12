package com.duoc.empresa_transportista_efs.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_efs.dto.OperacionEncoladaResponse;
import com.duoc.empresa_transportista_efs.dto.TipoOperacionGuia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaS3ProductorService {

	private final GuiaDespachoService guiaDespachoService;
	private final StagingEfsService stagingEfsService;
	private final GuiaProductorService guiaProductorService;

	public OperacionEncoladaResponse encolarSubida(String bucket, String key, String fecha, String transportista,
			String nombreGuia, MultipartFile file, boolean simularError) throws IOException {
		String messageId = UUID.randomUUID().toString();
		String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
		String stagingPath = stagingEfsService.guardarEnStaging(messageId, file);

		GuiaOperacionMensaje mensaje = GuiaOperacionMensaje.builder()
				.messageId(messageId)
				.tipo(TipoOperacionGuia.SUBIR_GUIA)
				.bucket(bucket)
				.key(resolvedKey)
				.stagingPath(stagingPath)
				.contentType(file.getContentType())
				.fecha(fecha)
				.transportista(transportista)
				.nombreGuia(nombreGuia)
				.simularError(simularError)
				.build();

		return guiaProductorService.encolar(mensaje);
	}

	public OperacionEncoladaResponse encolarActualizacion(String bucket, String key, String fecha,
			String transportista, String nombreGuia, MultipartFile file, boolean simularError) throws IOException {
		String messageId = UUID.randomUUID().toString();
		String oldKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
		String newKey = guiaDespachoService.buildActualizadoKey(fecha, transportista, nombreGuia);
		String stagingPath = stagingEfsService.guardarEnStaging(messageId, file);

		GuiaOperacionMensaje mensaje = GuiaOperacionMensaje.builder()
				.messageId(messageId)
				.tipo(TipoOperacionGuia.ACTUALIZAR_GUIA)
				.bucket(bucket)
				.key(newKey)
				.oldKey(oldKey)
				.newKey(newKey)
				.stagingPath(stagingPath)
				.contentType(file.getContentType())
				.fecha(fecha)
				.transportista(transportista)
				.nombreGuia(nombreGuia)
				.simularError(simularError)
				.build();

		return guiaProductorService.encolar(mensaje);
	}

	public OperacionEncoladaResponse encolarEliminacion(String bucket, String key, String fecha, String transportista,
			String nombreGuia, boolean simularError) {
		String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);

		GuiaOperacionMensaje mensaje = GuiaOperacionMensaje.builder()
				.messageId(UUID.randomUUID().toString())
				.tipo(TipoOperacionGuia.ELIMINAR_GUIA)
				.bucket(bucket)
				.key(resolvedKey)
				.fecha(fecha)
				.transportista(transportista)
				.nombreGuia(nombreGuia)
				.simularError(simularError)
				.build();

		return guiaProductorService.encolar(mensaje);
	}

	public OperacionEncoladaResponse encolarMovimiento(String bucket, String sourceKey, String destKey,
			boolean simularError) {
		GuiaOperacionMensaje mensaje = GuiaOperacionMensaje.builder()
				.messageId(UUID.randomUUID().toString())
				.tipo(TipoOperacionGuia.MOVER_GUIA)
				.bucket(bucket)
				.sourceKey(sourceKey)
				.destKey(destKey)
				.simularError(simularError)
				.build();

		return guiaProductorService.encolar(mensaje);
	}
}
