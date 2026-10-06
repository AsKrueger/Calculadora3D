# Primer Despliegue Funcional del Backend en AWS — 3D Cost Manager

Este documento detalla el procedimiento completo para construir, empaquetar, publicar en Amazon ECR y desplegar el backend Spring Boot de **3D Cost Manager** en una instancia Amazon EC2 conectada a Amazon RDS PostgreSQL y configurada mediante AWS Parameter Store.

---

## 1. Requisitos Previos y Validación Local

Antes de realizar cualquier build o despliegue en AWS, el código debe superar la verificación local:

```bash
cd backend
./mvnw.cmd clean verify
```

Debe finalizar con `BUILD SUCCESS`.

---

## 2. Construcción de la Imagen Docker (Soporte ARM64 / Graviton)

Dado que la instancia EC2 objetivo es `t4g.micro` (arquitectura ARM64 / Graviton), la imagen Docker debe compilarse específicamente para dicha arquitectura si el entorno de desarrollo es x86_64.

### Usando Docker Buildx (Multi-arch):
```bash
docker buildx create --use --name mybuilder
docker buildx build --platform linux/arm64 -t 3d-cost-manager-backend:v1.0.0 --load .
```

---

## 3. Publicación en Amazon ECR

1. **Autenticarse en ECR desde la terminal local:**
   ```bash
   aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com
   ```
2. **Etiquetar la imagen:**
   ```bash
   docker tag 3d-cost-manager-backend:v1.0.0 <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:v1.0.0
   docker tag 3d-cost-manager-backend:v1.0.0 <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:latest
   ```
3. **Subir la imagen (Push):**
   ```bash
   docker push <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:v1.0.0
   docker push <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:latest
   ```
4. **Documentar el Digest:** Guardar el digest SHA256 devuelto por ECR para trazabilidad.

---

## 4. Despliegue en la Instancia Amazon EC2

Acceder a la instancia EC2 (mediante SSM Session Manager o SSH con clave segura) y ejecutar los siguientes pasos:

1. **Autenticarse en ECR desde EC2 (usando el IAM Instance Profile):**
   ```bash
   aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com
   ```
2. **Descargar la imagen (Pull):**
   ```bash
   docker pull <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:v1.0.0
   ```
3. **Recuperar configuración desde Parameter Store e iniciar el contenedor:**
   ```bash
   # Obtener parámetros de AWS Parameter Store
   DB_URL=$(aws ssm get-parameter --name "/3d-cost-manager/database/url" --query "Parameter.Value" --output text --region eu-west-1)
   DB_USER=$(aws ssm get-parameter --name "/3d-cost-manager/database/username" --query "Parameter.Value" --output text --region eu-west-1)
   DB_PASS=$(aws ssm get-parameter --name "/3d-cost-manager/database/password" --with-decryption --query "Parameter.Value" --output text --region eu-west-1)
   JWT_SEC=$(aws ssm get-parameter --name "/3d-cost-manager/jwt/secret" --with-decryption --query "Parameter.Value" --output text --region eu-west-1)
   ESIOS_TOK=$(aws ssm get-parameter --name "/3d-cost-manager/esios/token" --with-decryption --query "Parameter.Value" --output text --region eu-west-1)

   # Detener versión anterior si existe
   docker stop cost-manager-backend || true
   docker rm cost-manager-backend || true

   # Arrancar nuevo contenedor
   docker run -d \
     --name cost-manager-backend \
     -p 8080:8080 \
     -e SPRING_DATASOURCE_URL="$DB_URL" \
     -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
     -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
     -e JWT_SECRET="$JWT_SEC" \
     -e ESIOS_API_TOKEN="$ESIOS_TOK" \
     --restart unless-stopped \
     <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:v1.0.0
   ```

---

## 5. Verificación del Despliegue (Health Check)

Comprobar que el servicio responde correctamente:

```bash
curl http://localhost:8080/api/v1/health
```

Respuesta esperada:
```json
{
  "status": "UP"
}
```

---

## 6. Procedimiento de Rollback

En caso de fallo en la nueva versión desplegada:

1. Detener el contenedor actual:
   ```bash
   docker stop cost-manager-backend
   docker rm cost-manager-backend
   ```
2. Arrancar la versión anterior estable usando su tag o digest previo:
   ```bash
   docker run -d \
     --name cost-manager-backend \
     -p 8080:8080 \
     -e SPRING_DATASOURCE_URL="$DB_URL" \
     -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
     -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
     -e JWT_SECRET="$JWT_SEC" \
     -e ESIOS_API_TOKEN="$ESIOS_TOK" \
     --restart unless-stopped \
     <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:v0.9.0
   ```
