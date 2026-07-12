# Guía de configuración — GitHub Actions (Semana 8)

Configuración de secrets y variables para el workflow [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml), que despliega **3 contenedores** en EC2:

| Contenedor | Puerto | Rol |
|------------|--------|-----|
| `rabbitmq` | 5672, 15672 | Message broker |
| `empresa-transportista-efs` | 8080 | Producer — encola operaciones |
| `empresa-transportista-consumer` | 8081 | Consumer — procesa cola, S3, Oracle |

## Flujo del workflow

1. Push a la rama `main`
2. Build y push de 2 imágenes Docker Hub:
   - `empresa-transportista-efs:latest` (producer)
   - `empresa-transportista-consumer:latest` (consumer)
3. SSH a EC2 (Elastic IP)
4. Copia `docker/docker-compose.ec2.yml` y genera `.env` con secrets
5. Ejecuta `docker compose -f docker-compose.ec2.yml up -d`
6. Levanta RabbitMQ + producer + consumer en red `guias-net`

## Secrets obligatorios

Configurar en: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `EC2_HOST` | Elastic IP de la instancia EC2 | `52.45.88.121` |
| `USER_SERVER` | Usuario SSH de EC2 | `ec2-user` |
| `EC2_SSH_KEY` | Contenido completo de `dsy2204-1.pem` | Ver [GITHUB_SECRETS_SSH.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/GITHUB_SECRETS_SSH.md) |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub | `tu_usuario` |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub | Generado en hub.docker.com |
| `AWS_ACCESS_KEY_ID` | Credencial temporal AWS Academy | Desde Learner Lab |
| `AWS_SECRET_ACCESS_KEY` | Credencial temporal AWS Academy | Desde Learner Lab |
| `AWS_SESSION_TOKEN` | Token de sesión AWS Academy | Desde Learner Lab |
| `AWS_S3_BUCKET` | Nombre del bucket S3 | `cdy2204-1` |
| `ORACLE_JDBC_URL` | URL JDBC hacia Oracle en OCI | `jdbc:oracle:thin:@(description=...)` |
| `ORACLE_USER` | Usuario de la base Oracle | `ADMIN` |
| `ORACLE_PASSWORD` | Password de la base Oracle | `tu_password` |

## Secrets opcionales

| Secret | Valor por defecto | Descripción |
|--------|-------------------|-------------|
| `RABBITMQ_USER` | `guias` | Usuario RabbitMQ (no usar `guest` entre contenedores) |
| `RABBITMQ_PASS` | `guias_secret` | Password RabbitMQ |
| `EFS_MOUNT_PATH` | `/home/ec2-user/efs` | Ruta donde EFS está montado en EC2 |
| `EFS_PATH` | `/app/efs` | Ruta EFS dentro de los contenedores |

## Cómo agregar un secret

1. Ir al repositorio en GitHub
2. **Settings** → **Secrets and variables** → **Actions**
3. Clic en **New repository secret**
4. Ingresar **Name** (exactamente como en la tabla) y **Secret** (valor)
5. **Add secret**

## Renovación de credenciales AWS Academy

Las credenciales temporales **caducan** al cerrar el Learner Lab o tras unas horas.

Cuando caduquen:

1. Abrir AWS Academy Learner Lab → **AWS Details** → copiar las 3 credenciales
2. Actualizar en GitHub Secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SESSION_TOKEN`
3. **Actions** → seleccionar el workflow fallido → **Re-run all jobs**

## Verificación post-deploy

### En GitHub Actions

- El job `Build and Deploy to EC2` debe terminar en verde
- En el paso **Deploy to EC2**, revisar que `docker compose ps` muestre 3 servicios `running`

### En EC2 (SSH manual)

```bash
ssh -i dsy2204-1.pem ec2-user@<ELASTIC_IP>
docker ps
```

Debes ver 3 contenedores:

```
rabbitmq
empresa-transportista-efs
empresa-transportista-consumer
```

### Endpoints de prueba

| Servicio | URL |
|----------|-----|
| RabbitMQ Management UI | `http://<ELASTIC_IP>:15672` (login: `guias` / `guias_secret`) |
| Producer | `http://<ELASTIC_IP>:8080` (requiere JWT) |
| Consumer | `http://<ELASTIC_IP>:8081/api/cola/estado` (requiere JWT) |

## Troubleshooting

### Error SSH `Permission denied (publickey)`

La clave en `EC2_SSH_KEY` no coincide con la instancia EC2. Ver guía detallada:
[Exp3_S8_lisbeth_bilbao_grupo_2_producer/GITHUB_SECRETS_SSH.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/GITHUB_SECRETS_SSH.md)

El workflow valida el fingerprint de `dsy2204-1.pem`:
`SHA256:LMpmI/thDtRMsbFMMYywOeJQuOh1Om2Tm9jZditomF4`

### Consumer no arranca

- Verificar `ORACLE_JDBC_URL`, `ORACLE_USER` y `ORACLE_PASSWORD`
- Ejecutar `Exp3_S8_lisbeth_bilbao_grupo_2_consumer/docs/oracle_guias_s8.sql` en Oracle antes del primer deploy

### Producer/Consumer no conectan a RabbitMQ

- Verificar que los 3 contenedores estén en la misma red (`guias-net`)
- Revisar logs: `docker logs rabbitmq`

## Referencias

- Infraestructura AWS: [config_aws_guide.md](config_aws_guide.md)
- Despliegue detallado: [Exp3_S8_lisbeth_bilbao_grupo_2_producer/DEPLOY_S8.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/DEPLOY_S8.md)
- Pruebas Postman: [Exp3_S8_lisbeth_bilbao_grupo_2_producer/POSTMAN_PRUEBAS.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/POSTMAN_PRUEBAS.md)
