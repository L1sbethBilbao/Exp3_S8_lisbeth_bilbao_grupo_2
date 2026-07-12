# Despliegue Semana 8 — RabbitMQ + Producer + Consumer

Arquitectura en EC2:

```
Postman → API Gateway → Producer :8080 (encola)
                              ↓
                         RabbitMQ :5672
                              ↓
                         Consumer :8081 (S3 + Oracle + EFS)
```

## Puertos en Security Group EC2

| Puerto | Servicio |
|--------|----------|
| 22 | SSH |
| 8080 | Producer (Spring Boot) |
| 8081 | Consumer (monitoreo cola/DLQ) |
| 15672 | RabbitMQ Management UI (opcional, para video) |

## Opción A — GitHub Actions (recomendado)

Push a `main` en el repo **producer** ejecuta `.github/workflows/deploy.yml`:

1. Compila y publica imagen `empresa-transportista-efs`
2. Intenta compilar consumer desde repo `CONSUMER_REPO` (o usa imagen ya publicada)
3. En EC2 levanta **rabbitmq**, **producer** y **consumer** en red Docker `guias-net`

### Secrets obligatorios (producer repo)

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
| `ORACLE_JDBC_URL` | JDBC Oracle OCI |
| `ORACLE_USER` | Usuario Oracle |
| `ORACLE_PASSWORD` | Password Oracle |

### Secrets opcionales

| Secret | Default | Descripción |
|--------|---------|-------------|
| `EFS_MOUNT_PATH` | `/home/ec2-user/efs` | Ruta EFS en EC2 |
| `EFS_PATH` | `/app/efs` | Ruta EFS en contenedor |
| `RABBITMQ_USER` | `guias` | Usuario RabbitMQ (no usar `guest` entre contenedores) |
| `RABBITMQ_PASS` | `guias_secret` | Password RabbitMQ |
| `CONSUMER_REPO` | `L1sbethBilbao/Exp3_S8_lisbeth_bilbao_grupo2-consumer` | Repo GitHub del consumer |

### Consumer en GitHub

Sube el proyecto consumer a GitHub y configura el mismo `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`.
El workflow del consumer (`.github/workflows/deploy.yml`) publica `empresa-transportista-consumer:latest`.

Si el consumer aún no está en GitHub, el deploy del producer intentará usar la imagen ya publicada en DockerHub.

## Opción B — Docker Compose manual en EC2

```bash
cd docker
export DOCKERHUB_USERNAME=tu_usuario
export AWS_S3_BUCKET=cdy2204-1
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...
export ORACLE_JDBC_URL=jdbc:oracle:thin:@...
export ORACLE_USER=...
export ORACLE_PASSWORD=...
docker compose -f docker-compose.ec2.yml pull
docker compose -f docker-compose.ec2.yml up -d
docker ps
```

## Opción C — Desarrollo local

Desde `producer/docker/`:

```bash
export AWS_S3_BUCKET=cdy2204-1
export ORACLE_JDBC_URL=jdbc:oracle:thin:@...
export ORACLE_USER=...
export ORACLE_PASSWORD=...
docker compose up -d --build
```

RabbitMQ UI: `http://localhost:15672` (usuario `guias` / `guias_secret` por defecto).

## Verificación post-deploy

```bash
# En EC2
docker ps
curl -s http://localhost:8080/api/pedidos   # requiere JWT
curl -s http://localhost:8081/api/cola/estado  # requiere JWT
```

En Postman: environment **Semana 8** → colección `Pruebas-Semana8` → Carpeta 1 (202 ENCOLADO) → Carpeta 2 (cola/DLQ).

## Oracle

Ejecutar antes del primer deploy:

```
consumer/docs/oracle_guias_s8.sql
```

## Referencias

- [GITHUB_SECRETS_SSH.md](GITHUB_SECRETS_SSH.md) — configuración SSH
- [AWS_SETUP.md](AWS_SETUP.md) — S3, EFS, IAM
- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — flujo de pruebas Semana 8
