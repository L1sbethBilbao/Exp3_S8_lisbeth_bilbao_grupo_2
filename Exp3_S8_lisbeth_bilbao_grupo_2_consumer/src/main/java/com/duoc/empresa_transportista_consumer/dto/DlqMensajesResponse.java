package com.duoc.empresa_transportista_consumer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DlqMensajesResponse {

	private String cola;
	private int total;
	private List<String> mensajes;
}
