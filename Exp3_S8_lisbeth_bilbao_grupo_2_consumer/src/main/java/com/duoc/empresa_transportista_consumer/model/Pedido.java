package com.duoc.empresa_transportista_consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {

	private String id;
	private String cliente;
	private String direccion;
	private String descripcion;
	private String transportista;
	private String fecha;
}
