# Guia de aprovisionamiento AWS

Pasos para configurar la infraestructura requerida por la actividad **Semana 8** (RabbitMQ + PostgreSQL + Producer + Consumer).

> Despliegue detallado: [DEPLOY_S8.md](DEPLOY_S8.md)

## 1. Bucket S3

1. En AWS Console → S3 → **Create bucket**
2. Nombre unico (ej. `guias-transportista-lisbeth-2026`)
3. Region: la misma que usaras para EC2 y EFS
4. Bloquear acceso publico (recomendado)
5. Anota el nombre y configuralo en:
   - `application.properties` → `aws.s3.bucket`
   - Secret de GitHub → `AWS_S3_BUCKET`

## 2. Amazon EFS

1. AWS Console → EFS → **Create file system**
2. VPC: la misma de tu instancia EC2
3. Crear **mount targets** en las subnets de EC2
4. Security group de EFS: permitir NFS (puerto 2049) desde el security group de EC2

## 3. Instancia EC2

1. Lanzar instancia Amazon Linux 2023 o Ubuntu
2. Asignar **Elastic IP**
3. Security group: abrir puertos **8080** (producer), **8081** (consumer), **5672** (RabbitMQ), **15672** (RabbitMQ UI) y **22** (SSH). PostgreSQL (5432) solo interno en Docker.
4. Asociar **IAM Role** con permisos S3 (ver seccion 4)
5. Misma VPC/subnet que EFS

### Instalar Docker en EC2

```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
```

## 4. IAM Role para EC2

Crear rol con policy (ajusta el nombre del bucket):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::tu-bucket-guias",
        "arn:aws:s3:::tu-bucket-guias/*"
      ]
    }
  ]
}
```

Asociar el rol a la instancia EC2. **No** hardcodear access keys en el codigo.

## 5. Montar EFS en EC2

```bash
# Amazon Linux - instalar cliente NFS
sudo yum install -y amazon-efs-utils

# Crear punto de montaje
sudo mkdir -p /home/ec2-user/efs

# Montar (reemplaza fs-xxxxx con tu ID de EFS)
sudo mount -t efs fs-XXXXXXXX:/ /home/ec2-user/efs

# Verificar
df -h
```

Para montaje persistente al reiniciar, agregar a `/etc/fstab`:

```
fs-XXXXXXXX:/ /home/ec2-user/efs efs defaults,_netdev 0 0
```

## 6. Despliegue en EC2 (Semana 8)

El workflow `.github/workflows/deploy.yml` levanta **RabbitMQ**, **PostgreSQL**, **producer** (`:8080`) y **consumer** (`:8081`) en red Docker `guias-net`.

Despliegue manual alternativo:

```bash
cd docker
docker compose -f docker-compose.ec2.yml up -d
```

Ver [DEPLOY_S8.md](DEPLOY_S8.md) para variables de entorno (RabbitMQ, PostgreSQL, AWS).

### Verificar cadena EFS (para el video)

```bash
# En EC2
df -h
ls -R /home/ec2-user/efs

# Dentro del contenedor
sudo docker exec -it empresa-transportista-efs bash
df -h
ls -R /app/efs
```

Flujo a explicar:

```
Microservicio → /app/efs → Linux EC2 /home/ec2-user/efs → Amazon EFS
```

## 8. Coleccion Postman

Importar en Postman:

```
postman/Pruebas-Semana8.postman_collection.json
postman/Semana8.postman_environment.example.json
```

Editar variables `ec2_host` y `bucket`. Ejecutar en orden:

1. **Carpeta 1 — GESTOR:** crear pedidos y encolar guías (**202 ENCOLADO**)
2. **Carpeta 2 — RabbitMQ:** estado cola/DLQ (consumer :8081)
3. **Carpeta 3 — LECTOR:** descarga PDF, POST/DELETE **403**

Ver [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) para el flujo completo y el guion del video.

## 9. Secrets de GitHub Actions

Configurar en el repositorio → Settings → Secrets:

| Secret | Descripcion |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub |
| `EC2_HOST` | IP elastica de EC2 (ej. 52.45.88.121) |
| `USER_SERVER` | `ec2-user` (Amazon Linux) |
| `EC2_SSH_KEY` | Clave privada **dsy2204-1.pem** completa |
| `AWS_ACCESS_KEY_ID` | Credencial temporal AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Credencial temporal AWS Academy |
| `AWS_SESSION_TOKEN` | Token de sesion AWS Academy |
| `AWS_REGION` | Region AWS (ej. us-east-1) |
| `AWS_S3_BUCKET` | Nombre del bucket S3 (ej. cdy2204-1) |
| `EFS_MOUNT_PATH` | Ruta EFS en EC2 (default: `/home/ec2-user/efs`) |
| `EFS_PATH` | Ruta EFS en contenedor (default: `/app/efs`) |
| `RABBITMQ_USER` | Usuario RabbitMQ (default: `guias`) |
| `RABBITMQ_PASS` | Password RabbitMQ (default: `guias_secret`) |
| `POSTGRES_DB` | Base PostgreSQL (default: `guias_db`) |
| `POSTGRES_USER` | Usuario PostgreSQL (default: `guias`) |
| `POSTGRES_PASSWORD` | Password PostgreSQL (default: `guias_secret`) |

**EFS:** montar en EC2 con `sudo mount -t efs fs-XXXXXXXX:/ /home/ec2-user/efs`. El deploy mapea `-v EFS_MOUNT_PATH:EFS_PATH` en producer y consumer.

**Nota:** La conexion SSH a EC2 usa `USER_SERVER` + `EC2_SSH_KEY`. Las credenciales AWS se pasan a los contenedores para S3. RabbitMQ y PostgreSQL corren en docker-compose dentro de EC2.

**Importante:** Las credenciales AWS Academy **expiran**. Cuando caduquen, actualiza los 3 secrets en GitHub y vuelve a ejecutar el workflow.

## Arquitectura del codigo (Semana 8)

```
Producer (:8080)
  PedidoController / AwsS3Controller
    └── GuiaProductorService / GuiaS3ProductorService
            └── RabbitMQ (guias.queue → guias.dlq)

Consumer (:8081)
  GuiaMensajeListener (@RabbitListener, ACK manual)
    └── GuiaProcesamientoService
            ├── GuiaGeneradorService (PDF)
            ├── EfsService + AwsS3Service
            └── GuiaPersistenciaService (PostgreSQL)
```

El bucket va en la URL como path variable `/{bucket}/`, igual que el proyecto del profesor.

Tras generar 2 guias (Postman carpeta 1), la estructura en EFS y S3 es:

```
20250604/TransportesSur/guia-PED-001.pdf
20250605/TransportesNorte/guia-PED-002.pdf
```

## Checklist antes de grabar el video

- [ ] EFS montado en `/home/ec2-user/efs` (`df -h` lo muestra)
- [ ] Contenedores corriendo: `rabbitmq`, `postgres-guias`, producer `:8080`, consumer `:8081`
- [ ] Bucket S3 creado y accesible desde EC2
- [ ] PostgreSQL: tabla `guias_despacho_s8` creada automaticamente (`docker/init/postgres_guias_s8.sql`)
- [ ] `POST .../generar-guia` retorna **202 ENCOLADO** y consumer persiste en S3
- [ ] `GET /api/cola/estado` (consumer) muestra cola procesada
- [ ] GitHub Actions despliega al hacer push a `main`
