package com.duoc.empresa_transportista_consumer.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_consumer.config.RabbitMQConfig;
import com.duoc.empresa_transportista_consumer.dto.ColaEstadoResponse;
import com.duoc.empresa_transportista_consumer.dto.DlqMensajesResponse;
import com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje;
import com.duoc.empresa_transportista_consumer.dto.ProcesamientoManualResponse;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColaMonitoreoService {

	private final RabbitTemplate rabbitTemplate;
	private final GuiaProcesamientoService guiaProcesamientoService;
	private final Jackson2JsonMessageConverter messageConverter;

	@Value("${spring.rabbitmq.host}")
	private String host;

	@Value("${spring.rabbitmq.port}")
	private int port;

	@Value("${spring.rabbitmq.username}")
	private String username;

	@Value("${spring.rabbitmq.password}")
	private String password;

	public ColaEstadoResponse obtenerEstadoColaPrincipal() {
		return rabbitTemplate.execute(channel -> {
			var declareOk = channel.queueDeclarePassive(RabbitMQConfig.MAIN_QUEUE);
			return ColaEstadoResponse.builder()
					.cola(RabbitMQConfig.MAIN_QUEUE)
					.mensajesPendientes(declareOk.getMessageCount())
					.consumidores(declareOk.getConsumerCount())
					.build();
		});
	}

	public ColaEstadoResponse obtenerEstadoDlq() {
		return rabbitTemplate.execute(channel -> {
			var declareOk = channel.queueDeclarePassive(RabbitMQConfig.DLQ_QUEUE);
			return ColaEstadoResponse.builder()
					.cola(RabbitMQConfig.DLQ_QUEUE)
					.mensajesPendientes(declareOk.getMessageCount())
					.consumidores(declareOk.getConsumerCount())
					.build();
		});
	}

	public DlqMensajesResponse listarMensajesDlq(int limite) {
		List<String> mensajes = new ArrayList<>();
		int max = Math.max(1, Math.min(limite, 50));

		ConnectionFactory factory = crearConnectionFactory();

		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
			for (int i = 0; i < max; i++) {
				GetResponse response = channel.basicGet(RabbitMQConfig.DLQ_QUEUE, false);
				if (response == null) {
					break;
				}
				String body = new String(response.getBody(), StandardCharsets.UTF_8);
				mensajes.add(body);
				channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);
			}
		} catch (Exception e) {
			log.error("Error al leer DLQ: {}", e.getMessage(), e);
			throw new IllegalStateException("No se pudo leer la DLQ: " + e.getMessage(), e);
		}

		return DlqMensajesResponse.builder()
				.cola(RabbitMQConfig.DLQ_QUEUE)
				.total(mensajes.size())
				.mensajes(mensajes)
				.build();
	}

	public ProcesamientoManualResponse procesarUnoManual() {
		ConnectionFactory factory = crearConnectionFactory();

		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
			GetResponse response = channel.basicGet(RabbitMQConfig.MAIN_QUEUE, false);
			if (response == null) {
				return ProcesamientoManualResponse.builder()
						.procesado(false)
						.mensaje("No hay mensajes en la cola principal")
						.build();
			}

			org.springframework.amqp.core.Message springMessage = new org.springframework.amqp.core.Message(
					response.getBody(), new org.springframework.amqp.core.MessageProperties());
			GuiaOperacionMensaje mensaje = (GuiaOperacionMensaje) messageConverter.fromMessage(springMessage);
			guiaProcesamientoService.procesar(mensaje);
			channel.basicAck(response.getEnvelope().getDeliveryTag(), false);

			return ProcesamientoManualResponse.builder()
					.procesado(true)
					.mensaje("Mensaje procesado manualmente: " + mensaje.getMessageId())
					.build();
		} catch (Exception e) {
			log.error("Error en procesamiento manual: {}", e.getMessage(), e);
			throw new IllegalStateException("Error al procesar mensaje manualmente: " + e.getMessage(), e);
		}
	}

	private ConnectionFactory crearConnectionFactory() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);
		return factory;
	}
}
