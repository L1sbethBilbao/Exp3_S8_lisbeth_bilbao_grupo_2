package com.duoc.empresa_transportista_consumer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_consumer.dto.PedidoMensajeData;
import com.duoc.empresa_transportista_consumer.dto.TipoOperacionGuia;
import com.duoc.empresa_transportista_consumer.model.Pedido;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaProcesamientoService {

	private final GuiaGeneradorService guiaGeneradorService;
	private final GuiaDespachoService guiaDespachoService;
	private final EfsService efsService;
	private final AwsS3Service awsS3Service;
	private final GuiaPersistenciaService guiaPersistenciaService;

	@Value("${aws.s3.bucket}")
	private String defaultBucket;

	public void procesar(GuiaOperacionMensaje mensaje) throws Exception {
		if (mensaje.isSimularError()) {
			throw new IllegalStateException("Error simulado para demostracion de DLQ");
		}

		switch (mensaje.getTipo()) {
			case GENERAR_GUIA -> procesarGenerar(mensaje);
			case SUBIR_GUIA -> procesarSubir(mensaje);
			case ACTUALIZAR_GUIA -> procesarActualizar(mensaje);
			case ELIMINAR_GUIA -> procesarEliminar(mensaje);
			case MOVER_GUIA -> procesarMover(mensaje);
			default -> throw new IllegalArgumentException("Tipo de operacion no soportado: " + mensaje.getTipo());
		}
	}

	private void procesarGenerar(GuiaOperacionMensaje mensaje) throws Exception {
		PedidoMensajeData data = mensaje.getPedido();
		if (data == null) {
			throw new IllegalArgumentException("Datos de pedido requeridos para GENERAR_GUIA");
		}

		Pedido pedido = Pedido.builder()
				.id(data.getId())
				.cliente(data.getCliente())
				.direccion(data.getDireccion())
				.descripcion(data.getDescripcion())
				.transportista(data.getTransportista())
				.fecha(data.getFecha())
				.build();

		String nombreGuia = guiaGeneradorService.nombreGuia(pedido.getId());
		String key = guiaDespachoService.buildKey(pedido.getFecha(), pedido.getTransportista(), nombreGuia);
		byte[] pdf = guiaGeneradorService.generarPdf(pedido);
		String bucket = resolveBucket(mensaje);

		efsService.saveBytes(key, pdf);
		awsS3Service.uploadBytes(bucket, key, pdf, "application/pdf");
		guiaPersistenciaService.guardarProcesado(mensaje, key, guiaDespachoService.obtenerNombreArchivo(key));
		log.info("GENERAR_GUIA procesada: pedido={}, key={}", pedido.getId(), key);
	}

	private void procesarSubir(GuiaOperacionMensaje mensaje) throws Exception {
		byte[] content = efsService.readBytes(mensaje.getStagingPath());
		String bucket = resolveBucket(mensaje);
		String key = mensaje.getKey();

		efsService.saveBytes(key, content);
		awsS3Service.uploadBytes(bucket, key, content, mensaje.getContentType());
		guiaPersistenciaService.guardarProcesado(mensaje, key, guiaDespachoService.obtenerNombreArchivo(key));
		efsService.deleteStagingDirectory(mensaje.getStagingPath());
		log.info("SUBIR_GUIA procesada: key={}", key);
	}

	private void procesarActualizar(GuiaOperacionMensaje mensaje) throws Exception {
		byte[] content = efsService.readBytes(mensaje.getStagingPath());
		String bucket = resolveBucket(mensaje);
		String newKey = mensaje.getNewKey();
		String oldKey = mensaje.getOldKey();

		efsService.saveBytes(newKey, content);
		awsS3Service.uploadBytes(bucket, newKey, content, mensaje.getContentType());
		efsService.deleteFile(oldKey);
		awsS3Service.deleteObject(bucket, oldKey);
		guiaPersistenciaService.actualizarS3Key(oldKey, newKey, guiaDespachoService.obtenerNombreArchivo(newKey));
		efsService.deleteStagingDirectory(mensaje.getStagingPath());
		log.info("ACTUALIZAR_GUIA procesada: {} -> {}", oldKey, newKey);
	}

	private void procesarEliminar(GuiaOperacionMensaje mensaje) throws Exception {
		String bucket = resolveBucket(mensaje);
		String key = mensaje.getKey();

		efsService.deleteFile(key);
		awsS3Service.deleteObject(bucket, key);
		guiaPersistenciaService.marcarEliminado(key);
		log.info("ELIMINAR_GUIA procesada: key={}", key);
	}

	private void procesarMover(GuiaOperacionMensaje mensaje) {
		String bucket = resolveBucket(mensaje);
		awsS3Service.moveObject(bucket, mensaje.getSourceKey(), mensaje.getDestKey());
		guiaPersistenciaService.actualizarS3Key(mensaje.getSourceKey(), mensaje.getDestKey(),
				guiaDespachoService.obtenerNombreArchivo(mensaje.getDestKey()));
		if (mensaje.getKey() == null) {
			mensaje.setKey(mensaje.getDestKey());
		}
		log.info("MOVER_GUIA procesada: {} -> {}", mensaje.getSourceKey(), mensaje.getDestKey());
	}

	private String resolveBucket(GuiaOperacionMensaje mensaje) {
		return mensaje.getBucket() != null && !mensaje.getBucket().isBlank() ? mensaje.getBucket() : defaultBucket;
	}
}
