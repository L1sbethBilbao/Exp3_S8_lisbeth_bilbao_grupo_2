package com.duoc.empresa_transportista_consumer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duoc.empresa_transportista_consumer.entity.GuiaDespachoEntity;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespachoEntity, Long> {

	boolean existsByMessageId(String messageId);

	Optional<GuiaDespachoEntity> findByMessageId(String messageId);

	Optional<GuiaDespachoEntity> findByS3Key(String s3Key);

	List<GuiaDespachoEntity> findByPedidoId(String pedidoId);
}
