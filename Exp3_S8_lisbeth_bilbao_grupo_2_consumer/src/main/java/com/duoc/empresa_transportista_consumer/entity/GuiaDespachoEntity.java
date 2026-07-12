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
@Table(name = "GUIAS_DESPACHO_S8")
public class GuiaDespachoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "MESSAGE_ID", nullable = false, unique = true, length = 36)
	private String messageId;

	@Column(name = "TIPO_OPERACION", nullable = false, length = 20)
	private String tipoOperacion;

	@Column(name = "PEDIDO_ID", length = 20)
	private String pedidoId;

	@Column(name = "CLIENTE", length = 100)
	private String cliente;

	@Column(name = "DIRECCION", length = 200)
	private String direccion;

	@Column(name = "DESCRIPCION", length = 500)
	private String descripcion;

	@Column(name = "TRANSPORTISTA", length = 100)
	private String transportista;

	@Column(name = "FECHA", length = 20)
	private String fecha;

	@Column(name = "S3_KEY", length = 300)
	private String s3Key;

	@Column(name = "NOMBRE_GUIA", length = 100)
	private String nombreGuia;

	@Column(name = "ESTADO", nullable = false, length = 20)
	private String estado;

	@Column(name = "FECHA_REGISTRO", nullable = false)
	private LocalDateTime fechaRegistro;
}
