# Automatización CI/CD (GitHub Actions → Amazon ECR) — 3D Cost Manager

Este documento detalla la arquitectura, el flujo de ejecución y la configuración del pipeline de Integración Continua y Despliegue Continuo (**CI/CD**) para el backend de **3D Cost Manager**.

---

## 1. Arquitectura del Pipeline

El pipeline automatiza la validación del código y la publicación de imágenes Docker optimizadas para arquitectura ARM64 (`linux/arm64`) en Amazon ECR, utilizando autenticación segura basada en **AWS OIDC** (sin credenciales permanentes de acceso).

```text
Developer (Git Push / PR)
       │
       ▼
GitHub Actions
       │
       ├── 1. Checkout repository
       ├── 2. Setup Java 21 (Temurin)
       └── 3. Run Maven Verify (`./mvnw clean verify`)
               │
               ├── [FAIL] ──► Detener pipeline (Cero publicación)
               │
               └── [PASS] ──► (Solo en Push a main/develop)
                                │
                                ├── 4. AWS OIDC Authentication
                                ├── 5. ECR Login
                                ├── 6. Docker Buildx (ARM64)
                                └── 7. Push Image to ECR (`<commit-sha>` + `latest`)
```

---

## 2. Triggers y Comportamiento (PR vs Push)

* **Pull Request:** Ejecuta únicamente la fase de verificación (`Maven clean verify` y tests), garantizando que ningún código roto o con tests fallidos pueda fusionarse. No publica imágenes en ECR.
* **Push a `develop` o `main`:** Ejecuta la fase de verificación y, si es exitosa, construye la imagen Docker para `linux/arm64` y la publica automáticamente en Amazon ECR.

---

## 3. Autenticación Segura (AWS OIDC)

Para evitar el uso de credenciales permanentes (`AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY`) en los secretos de GitHub, se utiliza **OpenID Connect (OIDC)**. GitHub Actions asume un rol de IAM en AWS en tiempo de ejecución de manera segura y efímera.

### Política IAM (Mínimo Privilegio para CI/CD):
El rol IAM asumido por el pipeline dispone exclusivamente de permisos para interactuar con el repositorio ECR `3d-cost-manager-backend`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "arn:aws:ecr:eu-west-1:ACCOUNT_ID:repository/3d-cost-manager-backend"
    }
  ]
}
```

---

## 4. Versionado y Trazabilidad

Cada imagen publicada en ECR queda inequívocamente vinculada al commit de Git que la generó:
* `3d-cost-manager-backend:<github.sha>` (Tag único e inmutable basado en el commit).
* `3d-cost-manager-backend:latest` (Referencia apuntando al último build exitoso).

Esto garantiza una trazabilidad absoluta entre el código fuente y el contenedor ejecutado en la infraestructura AWS.
