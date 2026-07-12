package com.duoc.empresa_transportista_consumer.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_consumer.dto.PedidoMensajeData;
import com.duoc.empresa_transportista_consumer.entity.GuiaDespachoEntity;
import com.duoc.empresa_transportista_consumer.repository.GuiaDespachoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuiaPersistenciaService {

	private final GuiaDespachoRepository repository;

	@Transactional
	public GuiaDespachoEntity guardarProcesado(GuiaOperacionMensaje mensaje, String s3Key, String nombreGuia) {
		PedidoMensajeData pedido = mensaje.getPedido();
		GuiaDespachoEntity entity = GuiaDespachoEntity.builder()
				.messageId(mensaje.getMessageId())
				.tipoOperacion(mensaje.getTipo().name())
				.pedidoId(pedido != null ? pedido.getId() : null)
				.cliente(pedido != null ? pedido.getCliente() : null)
				.direccion(pedido != null ? pedido.getDireccion() : null)
				.descripcion(pedido != null ? pedido.getDescripcion() : null)
				.transportista(resolveTransportista(mensaje, pedido))
				.fecha(resolveFecha(mensaje, pedido))
				.s3Key(s3Key)
				.nombreGuia(nombreGuia)
				.estado("PROCESADO")
				.fechaRegistro(LocalDateTime.now())
				.build();
		return repository.save(entity);
	}

	@Transactional
	public void actualizarS3Key(String oldKey, String newKey, String nombreGuia) {
		repository.findByS3Key(oldKey).ifPresent(entity -> {
			entity.setS3Key(newKey);
			if (nombreGuia != null) {
				entity.setNombreGuia(nombreGuia);
			}
			entity.setEstado("PROCESADO");
			repository.save(entity);
		});
	}

	@Transactional
	public void marcarEliminado(String s3Key) {
		repository.findByS3Key(s3Key).ifPresent(entity -> {
			entity.setEstado("ELIMINADO");
			repository.save(entity);
		});
	}

	private String resolveTransportista(GuiaOperacionMensaje mensaje, PedidoMensajeData pedido) {
		if (mensaje.getTransportista() != null && !mensaje.getTransportista().isBlank()) {
			return mensaje.getTransportista();
		}
		return pedido != null ? pedido.getTransportista() : null;
	}

	private String resolveFecha(GuiaOperacionMensaje mensaje, PedidoMensajeData pedido) {
		if (mensaje.getFecha() != null && !mensaje.getFecha().isBlank()) {
			return mensaje.getFecha();
		}
		return pedido != null ? pedido.getFecha() : null;
	}
}
