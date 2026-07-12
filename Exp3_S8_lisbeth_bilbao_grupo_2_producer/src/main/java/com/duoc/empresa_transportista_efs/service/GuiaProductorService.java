package com.duoc.empresa_transportista_efs.service;

import java.util.UUID;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.config.RabbitMQConfig;
import com.duoc.empresa_transportista_efs.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_efs.dto.OperacionEncoladaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaProductorService {

	private final RabbitTemplate rabbitTemplate;

	public OperacionEncoladaResponse encolar(GuiaOperacionMensaje mensaje) {
		if (mensaje.getMessageId() == null || mensaje.getMessageId().isBlank()) {
			mensaje.setMessageId(UUID.randomUUID().toString());
		}
		if (mensaje.getCorrelationId() == null || mensaje.getCorrelationId().isBlank()) {
			mensaje.setCorrelationId(mensaje.getMessageId());
		}

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIN_EXCHANGE, RabbitMQConfig.ROUTING_KEY, mensaje, msg -> {
			MessageProperties props = msg.getMessageProperties();
			props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			props.setMessageId(mensaje.getMessageId());
			props.setCorrelationId(mensaje.getCorrelationId());
			return msg;
		});

		log.info("Mensaje encolado: messageId={}, tipo={}", mensaje.getMessageId(), mensaje.getTipo());

		return OperacionEncoladaResponse.builder()
				.messageId(mensaje.getMessageId())
				.tipo(mensaje.getTipo())
				.estado("ENCOLADO")
				.mensaje("Operacion encolada para procesamiento asincrono")
				.build();
	}
}
