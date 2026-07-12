package com.duoc.empresa_transportista_consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaOperacionMensaje {

	private String messageId;
	private String correlationId;
	private TipoOperacionGuia tipo;
	private PedidoMensajeData pedido;
	private String bucket;
	private String key;
	private String oldKey;
	private String newKey;
	private String sourceKey;
	private String destKey;
	private String stagingPath;
	private String contentType;
	private String fecha;
	private String transportista;
	private String nombreGuia;
	private boolean simularError;
}
