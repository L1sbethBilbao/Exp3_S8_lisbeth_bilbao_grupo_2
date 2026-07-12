package com.duoc.empresa_transportista_consumer.listener;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import com.duoc.empresa_transportista_consumer.config.RabbitMQConfig;
import com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_consumer.repository.GuiaDespachoRepository;
import com.duoc.empresa_transportista_consumer.service.GuiaProcesamientoService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuiaMensajeListener {

	private final GuiaProcesamientoService guiaProcesamientoService;
	private final GuiaDespachoRepository guiaDespachoRepository;
	private final Jackson2JsonMessageConverter messageConverter;

	@RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE, ackMode = "MANUAL")
	public void procesarMensaje(Message message, Channel channel) throws IOException {
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		GuiaOperacionMensaje mensaje = null;
		try {
			mensaje = (GuiaOperacionMensaje) messageConverter.fromMessage(message);
			log.info("Mensaje recibido: messageId={}, tipo={}", mensaje.getMessageId(), mensaje.getTipo());

			if (guiaDespachoRepository.existsByMessageId(mensaje.getMessageId())) {
				log.info("Mensaje ya procesado (idempotencia): {}", mensaje.getMessageId());
				channel.basicAck(deliveryTag, false);
				return;
			}

			guiaProcesamientoService.procesar(mensaje);
			channel.basicAck(deliveryTag, false);
			log.info("ACK enviado para messageId={}", mensaje.getMessageId());
		} catch (Exception e) {
			log.error("Error procesando mensaje: {}", e.getMessage(), e);
			channel.basicNack(deliveryTag, false, false);
			log.warn("NACK enviado - mensaje enrutado a DLQ");
		}
	}
}
