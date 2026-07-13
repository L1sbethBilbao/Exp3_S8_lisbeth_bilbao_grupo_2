-- Tabla para Experiencia 3 Semana 8 (PostgreSQL)
CREATE TABLE IF NOT EXISTS guias_despacho_s8 (
  id              BIGSERIAL PRIMARY KEY,
  message_id      VARCHAR(36)  NOT NULL,
  tipo_operacion  VARCHAR(20)  NOT NULL,
  pedido_id       VARCHAR(20),
  cliente         VARCHAR(100),
  direccion       VARCHAR(200),
  descripcion     VARCHAR(500),
  transportista   VARCHAR(100),
  fecha           VARCHAR(20),
  s3_key          VARCHAR(300),
  nombre_guia     VARCHAR(100),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'PROCESADO',
  fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_guias_s8_message_id UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_guias_s8_s3_key ON guias_despacho_s8 (s3_key);
CREATE INDEX IF NOT EXISTS idx_guias_s8_pedido ON guias_despacho_s8 (pedido_id);
