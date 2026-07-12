# Secrets de GitHub Actions — Semana 8

## SSH (obligatorio para deploy EC2)

| Secret | Valor |
|--------|-------|
| `EC2_HOST` | `52.45.88.121` |
| `USER_SERVER` | `ec2-user` |
| `EC2_SSH_KEY` | Contenido completo de `dsy2204-1.pem` |

Ver [GITHUB_SECRETS_SSH.md](GITHUB_SECRETS_SSH.md) para validar fingerprint y troubleshooting SSH.

## Docker Hub

| Secret | Descripción |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso |

Imágenes publicadas:
- `empresa-transportista-efs:latest` (producer)
- `empresa-transportista-consumer:latest` (consumer)

## AWS (credenciales temporales Academy)

| Secret | Descripción |
|--------|-------------|
| `AWS_ACCESS_KEY_ID` | Credencial temporal |
| `AWS_SECRET_ACCESS_KEY` | Credencial temporal |
| `AWS_SESSION_TOKEN` | Token de sesión |
| `AWS_S3_BUCKET` | Bucket S3 (ej. `cdy2204-1`) |

## EFS

| Secret | Default | Descripción |
|--------|---------|-------------|
| `EFS_MOUNT_PATH` | `/home/ec2-user/efs` | Ruta montaje en EC2 |
| `EFS_PATH` | `/app/efs` | Ruta dentro del contenedor |

## RabbitMQ (Semana 8)

| Secret | Default | Descripción |
|--------|---------|-------------|
| `RABBITMQ_USER` | `guias` | Usuario RabbitMQ (evitar `guest` entre contenedores) |
| `RABBITMQ_PASS` | `guias_secret` | Password RabbitMQ |

## Oracle OCI (consumer)

| Secret | Descripción |
|--------|-------------|
| `ORACLE_JDBC_URL` | `jdbc:oracle:thin:@(description=...)` |
| `ORACLE_USER` | Usuario BD |
| `ORACLE_PASSWORD` | Password BD |

Ejecutar `consumer/docs/oracle_guias_s8.sql` antes del primer deploy.

## Repositorios

Monorepo único: [Exp3_S8_lisbeth_bilbao_grupo_2](https://github.com/L1sbethBilbao/Exp3_S8_lisbeth_bilbao_grupo_2)

| Carpeta | Contenido |
|---------|-----------|
| `Exp3_S8_lisbeth_bilbao_grupo_2_producer/` | Producer + Postman + docs |
| `Exp3_S8_lisbeth_bilbao_grupo_2_consumer/` | Consumer + Oracle SQL |

El workflow `.github/workflows/deploy.yml` en la raíz del monorepo compila y despliega ambos servicios.

## Cuando caducan credenciales AWS

Actualiza `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` en GitHub Secrets y re-ejecuta el workflow.
