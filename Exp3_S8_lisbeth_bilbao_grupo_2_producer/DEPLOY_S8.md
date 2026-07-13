# Despliegue Semana 8 — RabbitMQ + PostgreSQL + Producer + Consumer

Arquitectura en EC2 (todo autocontenido):

```
Postman → API Gateway → Producer :8080 (encola)
                              ↓
                         RabbitMQ :5672
                              ↓
                         Consumer :8081 (S3 + PostgreSQL + EFS)
                              ↑
                    postgres-guias :5432 (interno)
```

## Puertos en Security Group EC2

| Puerto | Servicio |
|--------|----------|
| 22 | SSH |
| 8080 | Producer (Spring Boot) |
| 8081 | Consumer (monitoreo cola/DLQ) |
| 5672 | RabbitMQ AMQP |
| 15672 | RabbitMQ Management UI (opcional, para video) |

> PostgreSQL (5432) solo accesible dentro de la red Docker. No abrir en security group.

## Opción A — GitHub Actions (recomendado)

Push a `main` ejecuta `.github/workflows/deploy.yml`:

1. Compila y publica imagen `empresa-transportista-efs` (producer)
2. Compila y publica imagen `empresa-transportista-consumer` (consumer)
3. En EC2 levanta **rabbitmq**, **postgres**, **producer** y **consumer** en red Docker `guias-net`

### Secrets obligatorios

| Secret | Descripción |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token Docker Hub |
| `EC2_HOST` | IP elástica EC2 |
| `USER_SERVER` | `ec2-user` |
| `EC2_SSH_KEY` | Contenido de `dsy2204-1.pem` |
| `AWS_ACCESS_KEY_ID` | Credencial AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS Academy |
| `AWS_SESSION_TOKEN` | Token sesión AWS Academy |
| `AWS_S3_BUCKET` | Bucket S3 (ej. `cdy2204-1`) |

### Secrets opcionales

| Secret | Default | Descripción |
|--------|---------|-------------|
| `EFS_MOUNT_PATH` | `/home/ec2-user/efs` | Ruta EFS en EC2 |
| `EFS_PATH` | `/app/efs` | Ruta EFS en contenedor |
| `RABBITMQ_USER` | `guias` | Usuario RabbitMQ |
| `RABBITMQ_PASS` | `guias_secret` | Password RabbitMQ |
| `POSTGRES_DB` | `guias_db` | Base PostgreSQL |
| `POSTGRES_USER` | `guias` | Usuario PostgreSQL |
| `POSTGRES_PASSWORD` | `guias_secret` | Password PostgreSQL |

## Opción B — Docker Compose manual en EC2

```bash
cd docker
export DOCKERHUB_USERNAME=tu_usuario
export AWS_S3_BUCKET=cdy2204-1
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...
docker compose -f docker-compose.ec2.yml pull
docker compose -f docker-compose.ec2.yml up -d
docker ps
```

## Opción C — Desarrollo local

Desde `producer/docker/`:

```bash
export AWS_S3_BUCKET=cdy2204-1
docker compose up -d --build
```

RabbitMQ UI: `http://localhost:15672` (usuario `guias` / `guias_secret` por defecto).

## Verificación post-deploy

```bash
# En EC2
docker ps   # 4 contenedores
docker exec postgres-guias psql -U guias -d guias_db -c "\dt"
curl -s http://localhost:8080/api/pedidos   # requiere JWT
curl -s http://localhost:8081/api/cola/estado  # requiere JWT
```

En Postman: environment **Semana 8** → colección `Pruebas-Semana8` → Carpeta 1 (202 ENCOLADO) → Carpeta 2 (cola/DLQ).

## PostgreSQL

La tabla `guias_despacho_s8` se crea automáticamente al primer arranque del contenedor `postgres-guias` usando `docker/init/postgres_guias_s8.sql`.

Script de referencia: `Exp3_S8_lisbeth_bilbao_grupo_2_consumer/docs/postgres_guias_s8.sql`

## Referencias

- [config_github_guide.md](../config_github_guide.md) — secrets GitHub Actions
- [config_aws_guide.md](../config_aws_guide.md) — infraestructura AWS
- [GITHUB_SECRETS_SSH.md](GITHUB_SECRETS_SSH.md) — configuración SSH
- [AWS_SETUP.md](AWS_SETUP.md) — S3, EFS, IAM
- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — flujo de pruebas Semana 8
