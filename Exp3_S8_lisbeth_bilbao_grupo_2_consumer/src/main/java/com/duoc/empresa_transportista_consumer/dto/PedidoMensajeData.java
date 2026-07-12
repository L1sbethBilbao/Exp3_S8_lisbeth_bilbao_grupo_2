package com.duoc.empresa_transportista_consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PedidoMensajeData {

	private String id;
	private String cliente;
	private String direccion;
	private String descripcion;
	private String transportista;
	private String fecha;
}
