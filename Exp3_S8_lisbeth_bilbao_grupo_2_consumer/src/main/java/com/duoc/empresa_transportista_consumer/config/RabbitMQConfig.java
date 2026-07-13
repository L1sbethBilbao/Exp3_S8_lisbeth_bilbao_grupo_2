package com.duoc.empresa_transportista_consumer.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Configuration;

import com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

	public static final String MAIN_EXCHANGE = "guias.exchange";
	public static final String MAIN_QUEUE = "guias.queue";
	public static final String DLX_EXCHANGE = "guias.dlx";
	public static final String DLQ_QUEUE = "guias.dlq";
	public static final String ROUTING_KEY = "guias.routing";

	@Value("${spring.rabbitmq.host}")
	private String host;

	@Value("${spring.rabbitmq.port}")
	private int port;

	@Value("${spring.rabbitmq.username}")
	private String username;

	@Value("${spring.rabbitmq.password}")
	private String password;

	@Bean
	Jackson2JsonMessageConverter messageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
		// Producer publica __TypeId__ del paquete efs; el consumer debe mapearlo a su DTO local.
		Map<String, Class<?>> idClassMapping = new HashMap<>();
		idClassMapping.put(
				"com.duoc.empresa_transportista_efs.dto.GuiaOperacionMensaje",
				GuiaOperacionMensaje.class);
		idClassMapping.put(
				"com.duoc.empresa_transportista_consumer.dto.GuiaOperacionMensaje",
				GuiaOperacionMensaje.class);
		typeMapper.setIdClassMapping(idClassMapping);
		typeMapper.addTrustedPackages(
				"com.duoc.empresa_transportista_efs.dto",
				"com.duoc.empresa_transportista_consumer.dto");
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}

	@Bean
	CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory factory = new CachingConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);
		return factory;
	}

	@Bean
	RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory,
			Jackson2JsonMessageConverter messageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		return template;
	}

	@Bean
	DirectExchange mainExchange() {
		return new DirectExchange(MAIN_EXCHANGE, true, false);
	}

	@Bean
	DirectExchange deadLetterExchange() {
		return new DirectExchange(DLX_EXCHANGE, true, false);
	}

	@Bean
	Queue mainQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-dead-letter-exchange", DLX_EXCHANGE);
		args.put("x-dead-letter-routing-key", DLQ_QUEUE);
		return new Queue(MAIN_QUEUE, true, false, false, args);
	}

	@Bean
	Queue deadLetterQueue() {
		return new Queue(DLQ_QUEUE, true, false, false);
	}

	@Bean
	Binding mainBinding(Queue mainQueue, DirectExchange mainExchange) {
		return BindingBuilder.bind(mainQueue).to(mainExchange).with(ROUTING_KEY);
	}

	@Bean
	Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
		return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_QUEUE);
	}
}
