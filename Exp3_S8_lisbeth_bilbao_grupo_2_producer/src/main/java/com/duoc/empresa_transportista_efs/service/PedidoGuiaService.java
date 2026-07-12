package com.duoc.empresa_transportista_efs.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_efs.dto.OperacionEncoladaResponse;
import com.duoc.empresa_transportista_efs.dto.PedidoMensajeData;
import com.duoc.empresa_transportista_efs.dto.TipoOperacionGuia;
import com.duoc.empresa_transportista_efs.exception.GuiaYaGeneradaException;
import com.duoc.empresa_transportista_efs.model.Pedido;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoGuiaService {

	private final PedidoService pedidoService;
	private final GuiaProductorService guiaProductorService;

	public OperacionEncoladaResponse generarGuia(String pedidoId, boolean simularError) {
		Pedido pedido = pedidoService.obtenerPedido(pedidoId);

		if (pedido.isGuiaGenerada()) {
			throw new GuiaYaGeneradaException(pedidoId);
		}
		if ("PENDIENTE".equals(pedido.getEstadoGuia())) {
			throw new GuiaYaGeneradaException("La guia del pedido " + pedidoId + " ya esta en cola de procesamiento");
		}

		pedidoService.marcarGuiaPendiente(pedido);

		PedidoMensajeData pedidoData = PedidoMensajeData.builder()
				.id(pedido.getId())
				.cliente(pedido.getCliente())
				.direccion(pedido.getDireccion())
				.descripcion(pedido.getDescripcion())
				.transportista(pedido.getTransportista())
				.fecha(pedido.getFecha())
				.build();

		GuiaOperacionMensaje mensaje = GuiaOperacionMensaje.builder()
				.messageId(UUID.randomUUID().toString())
				.tipo(TipoOperacionGuia.GENERAR_GUIA)
				.pedido(pedidoData)
				.simularError(simularError)
				.build();

		return guiaProductorService.encolar(mensaje);
	}
}
