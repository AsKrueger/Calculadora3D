# Despliegue Automatizado AWS (ECR → EC2) — 3D Cost Manager

Este documento detalla la automatización del despliegue continuo (**CD**) implementada mediante **GitHub Actions** y **AWS Systems Manager (SSM) Run Command**, permitiendo actualizar el backend en la instancia EC2 sin exponer puertos SSH ni almacenar credenciales estáticas.

---

## 1. Arquitectura del Despliegue Automatizado

```text
GitHub (Push a develop / main)
       │
       ▼
GitHub Actions (CI Pipeline)
       │
       ├── 1. Maven Verify & Tests
       ├── 2. AWS OIDC Authentication
       ├── 3. Docker Buildx (ARM64)
       └── 4. Push Image to ECR (`<commit-sha>` + `latest`)
               │
               ▼
AWS Systems Manager (SSM) Run Command
       │
       ▼
Amazon EC2 Instance
       │
       ├── 1. AWS ECR Login
       ├── 2. Docker Pull (`<commit-sha>`)
       ├── 3. Fetch Secrets from AWS Parameter Store (`/3d-cost-manager/*`)
       ├── 4. Stop & Remove Old Container
       ├── 5. Run New Container (Port 8080)
       └── 6. Health Check (`GET /api/v1/health` → `200 UP`)
```

---

## 2. Configuración de Secretos en GitHub

Para que el pipeline pueda autenticarse de forma segura en AWS y comunicarse con la instancia EC2, se deben configurar dos secretos en el repositorio de GitHub (`Settings > Secrets and variables > Actions`):
1. **`AWS_ROLE_ARN`**: El ARN del rol IAM con confianza OIDC configurada para GitHub Actions y permisos de ECR push/SSM send-command.
2. **`AWS_EC2_INSTANCE_ID`**: El ID de la instancia EC2 objetivo (ej. `i-0123456789abcdef0`).

---

## 3. Procedimiento de Rollback Automatizado / Manual

Si una versión desplegada no supera el health check o presenta fallos en producción:
1. Localizar el digest o tag del commit anterior estable en **Amazon ECR**.
2. Ejecutar manualmente en EC2 (o mediante SSM Run Command) el despliegue apuntando al tag anterior:
   ```bash
   docker stop cost-manager-backend || true
   docker rm cost-manager-backend || true
   docker run -d \
     --name cost-manager-backend \
     -p 8080:8080 \
     -e SPRING_DATASOURCE_URL="$DB_URL" \
     -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
     -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
     -e JWT_SECRET="$JWT_SEC" \
     -e ESIOS_API_TOKEN="$ESIOS_TOK" \
     --restart unless-stopped \
     <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/3d-cost-manager-backend:<previous-commit-sha>
   ```
