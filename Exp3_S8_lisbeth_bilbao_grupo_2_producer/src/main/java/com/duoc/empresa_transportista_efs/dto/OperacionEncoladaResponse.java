package com.duoc.empresa_transportista_efs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperacionEncoladaResponse {

	private String messageId;
	private TipoOperacionGuia tipo;
	private String estado;
	private String mensaje;
}
