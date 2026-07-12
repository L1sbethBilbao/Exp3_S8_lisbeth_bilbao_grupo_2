# Guía de configuración — AWS (Semana 8)

Pasos para aprovisionar la infraestructura AWS requerida por la arquitectura Semana 8: **RabbitMQ + Producer + Consumer** en EC2, expuestos por **Elastic IP**, con integración a S3, EFS, API Gateway y Oracle OCI.

```
Postman → Azure IDaaS (token) → API Gateway → Producer :8080
                                              ↓
                                         RabbitMQ :5672
                                              ↓
                                         Consumer :8081 → S3 + Oracle OCI + EFS
```

## 1. Instancia EC2

1. AWS Console → **EC2** → **Launch instance**
2. AMI recomendada: **Amazon Linux 2023**
3. Tipo: `t2.micro` o superior
4. Key pair: `dsy2204-1` (la misma que usas en `EC2_SSH_KEY` de GitHub)
5. Misma VPC/subnet que EFS

### Elastic IP

1. EC2 → **Elastic IPs** → **Allocate Elastic IP address**
2. **Associate** con la instancia EC2
3. Usar esta IP en:
   - GitHub Secret `EC2_HOST`
   - Postman environment (`ec2_host`)
   - API Gateway integration (backend EC2)

## 2. Security Group — puertos inbound

Crear o editar el security group de la instancia EC2:

| Puerto | Protocolo | Origen | Servicio |
|--------|-----------|--------|----------|
| 22 | TCP | Tu IP / 0.0.0.0/0 | SSH (deploy GitHub Actions) |
| 5672 | TCP | 0.0.0.0/0 | RabbitMQ AMQP |
| 15672 | TCP | 0.0.0.0/0 | RabbitMQ Management UI |
| 8080 | TCP | 0.0.0.0/0 | Producer (Spring Boot) |
| 8081 | TCP | 0.0.0.0/0 | Consumer (monitoreo cola/DLQ) |

> Para el video de evaluación, abre 15672 y 5672 para evidenciar RabbitMQ Management UI desde Postman o navegador.

### EFS (security group separado)

El security group de EFS debe permitir **NFS puerto 2049** desde el security group de EC2.

## 3. Docker en EC2

Conectarse por SSH e instalar Docker + Compose:

```bash
ssh -i dsy2204-1.pem ec2-user@<ELASTIC_IP>

sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Plugin Docker Compose V2
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Verificar (reconectar SSH después de usermod)
docker compose version
```

## 4. Bucket S3

1. AWS Console → **S3** → **Create bucket**
2. Nombre único (ej. `cdy2204-1`)
3. Región: misma que EC2 (`us-east-1`)
4. Bloquear acceso público (recomendado)
5. Configurar en GitHub Secret `AWS_S3_BUCKET`

## 5. Amazon EFS

1. AWS Console → **EFS** → **Create file system**
2. VPC: la misma de la instancia EC2
3. Crear **mount targets** en las subnets de EC2
4. Security group EFS: NFS puerto 2049 desde EC2

### Montar EFS en EC2

```bash
sudo yum install -y amazon-efs-utils
sudo mkdir -p /home/ec2-user/efs
sudo mount -t efs fs-XXXXXXXX:/ /home/ec2-user/efs
df -h
```

Montaje persistente en `/etc/fstab`:

```
fs-XXXXXXXX:/ /home/ec2-user/efs efs defaults,_netdev 0 0
```

El deploy mapea EFS a los contenedores: `/home/ec2-user/efs` → `/app/efs`

## 6. IAM Role para EC2 (opcional)

Alternativa a pasar credenciales AWS por variables de entorno. Crear rol con policy S3:

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

Asociar el rol a la instancia EC2. Si usas credenciales AWS Academy, configúralas como secrets en GitHub (ver [config_github_guide.md](config_github_guide.md)).

## 7. API Gateway + Azure IDaaS

El tráfico de Postman entra por **API Gateway** (no directo a EC2 en el flujo de evaluación). El Gateway valida el JWT de Azure AD B2C y reenvía al producer/consumer en EC2.

Configuración detallada:
[Exp3_S8_lisbeth_bilbao_grupo_2_producer/docs/AWS_GATEWAY_SETUP.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/docs/AWS_GATEWAY_SETUP.md)

## 8. Oracle OCI (base de datos externa)

El consumer persiste guías en Oracle Cloud (OCI).

### Antes del primer deploy

Ejecutar el script SQL en Oracle:

```
Exp3_S8_lisbeth_bilbao_grupo_2_consumer/docs/oracle_guias_s8.sql
```

### Configuración en GitHub Secrets

| Secret | Descripción |
|--------|-------------|
| `ORACLE_JDBC_URL` | URL JDBC completa hacia Oracle OCI |
| `ORACLE_USER` | Usuario de la base |
| `ORACLE_PASSWORD` | Password |

### Red

Asegurar que la instancia EC2 puede conectarse a Oracle OCI (security lists / ACL en OCI que permitan el tráfico desde la IP elástica de EC2).

## 9. Despliegue automático (GitHub Actions)

El workflow en `.github/workflows/deploy.yml` ejecuta en cada push a `main`:

```bash
cd ~/app-s8
docker compose -f docker-compose.ec2.yml pull
docker compose -f docker-compose.ec2.yml up -d
```

### Despliegue manual alternativo

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

## 10. Verificación

```bash
# En EC2
docker ps                                    # 3 contenedores running
curl -s http://localhost:15672             # RabbitMQ UI
curl -s http://localhost:8081/api/cola/estado  # requiere JWT

# Cadena EFS
df -h
ls -R /home/ec2-user/efs
docker exec empresa-transportista-efs ls -la /app/efs
```

Flujo EFS para el video:

```
Microservicio → /app/efs → EC2 /home/ec2-user/efs → Amazon EFS
```

## Checklist pre-video

- [ ] Elastic IP asignada y asociada a EC2
- [ ] Security group con puertos 22, 5672, 15672, 8080, 8081
- [ ] EFS montado en `/home/ec2-user/efs` (`df -h` lo muestra)
- [ ] Docker y `docker compose` instalados en EC2
- [ ] Bucket S3 creado y accesible
- [ ] Oracle: tabla `GUIAS_DESPACHO_S8` creada
- [ ] 3 contenedores corriendo: `rabbitmq`, producer `:8080`, consumer `:8081`
- [ ] RabbitMQ UI accesible en `http://<ELASTIC_IP>:15672`
- [ ] `POST .../generar-guia` retorna **202 ENCOLADO**
- [ ] GitHub Actions despliega al hacer push a `main`

## Referencias

- Secrets GitHub: [config_github_guide.md](config_github_guide.md)
- Despliegue: [Exp3_S8_lisbeth_bilbao_grupo_2_producer/DEPLOY_S8.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/DEPLOY_S8.md)
- Postman: [Exp3_S8_lisbeth_bilbao_grupo_2_producer/POSTMAN_PRUEBAS.md](Exp3_S8_lisbeth_bilbao_grupo_2_producer/POSTMAN_PRUEBAS.md)
