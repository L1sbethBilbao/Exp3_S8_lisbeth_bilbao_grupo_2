package com.duoc.empresa_transportista_consumer.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "guias_despacho_s8")
public class GuiaDespachoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "message_id", nullable = false, unique = true, length = 36)
	private String messageId;

	@Column(name = "tipo_operacion", nullable = false, length = 20)
	private String tipoOperacion;

	@Column(name = "pedido_id", length = 20)
	private String pedidoId;

	@Column(name = "cliente", length = 100)
	private String cliente;

	@Column(name = "direccion", length = 200)
	private String direccion;

	@Column(name = "descripcion", length = 500)
	private String descripcion;

	@Column(name = "transportista", length = 100)
	private String transportista;

	@Column(name = "fecha", length = 20)
	private String fecha;

	@Column(name = "s3_key", length = 300)
	private String s3Key;

	@Column(name = "nombre_guia", length = 100)
	private String nombreGuia;

	@Column(name = "estado", nullable = false, length = 20)
	private String estado;

	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;
}
