package com.duoc.empresa_transportista_consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.duoc.empresa_transportista_consumer.repository.GuiaDespachoRepository;

@SpringBootTest
class EmpresaTransportistaConsumerApplicationTests {

	@MockBean
	private JwtDecoder jwtDecoder;

	@MockBean
	private GuiaDespachoRepository guiaDespachoRepository;

	@Test
	void contextLoads() {
	}
}
