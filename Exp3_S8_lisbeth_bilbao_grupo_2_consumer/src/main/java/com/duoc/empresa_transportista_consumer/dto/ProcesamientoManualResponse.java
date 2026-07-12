package com.duoc.empresa_transportista_consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcesamientoManualResponse {

	private boolean procesado;
	private String mensaje;
}
