# Configuración de Amazon EC2 (Compute) — 3D Cost Manager

Este documento detalla la configuración de la instancia **Amazon EC2**, el rol de IAM asociado (Instance Profile), la instalación automática de Docker y la integración de red para el backend de **3D Cost Manager**.

---

## 1. Especificaciones de la Instancia

| Parámetro | Valor Configurado | Justificación |
| :--- | :--- | :--- |
| **Tipo de Instancia** | `t4g.micro` | Procesadores AWS Graviton (ARM64), excelente rendimiento por vatio, bajo coste y elegible en Free Tier. |
| **AMI** | Amazon Linux 2023 (ARM64) | Sistema operativo moderno, ligero, optimizado para AWS y con soporte nativo de SSM y Docker. |
| **Subnet** | Public Subnet | Permite exposición controlada en el puerto `8080` para la API REST. |
| **Security Group** | `3d-cost-manager-ec2-sg` | Expone únicamente el puerto `8080` (API) y opcionalmente SSH o AWS Systems Manager. |

---

## 2. Seguridad y IAM (Mínimo Privilegio)

La instancia EC2 **no almacena credenciales estáticas**. Utiliza un **IAM Instance Profile** (`3d-cost-manager-ec2-instance-profile`) vinculado al rol `3d-cost-manager-ec2-role` que otorga permisos acotados exclusivamente a:
1. **Amazon ECR:** Permite autenticarse (`ecr:GetAuthorizationToken`) y descargar (`BatchGetImage`, `GetDownloadUrlForLayer`) la imagen Docker privada del repositorio `3d-cost-manager-backend`.
2. **Parameter Store:** Permite leer únicamente los parámetros bajo el path `/3d-cost-manager/*` en AWS Systems Manager Parameter Store.

Cero privilegios administrativos (`AdministratorAccess`).

---

## 3. Automatización (User Data)

Durante el primer arranque de la instancia, el script de **User Data** de CloudFormation ejecuta automáticamente:
1. Actualización del sistema (`yum update -y`).
2. Instalación del motor de contenedores Docker (`yum install -y docker`).
3. Inicio y habilitación del servicio Docker (`systemctl start docker`, `systemctl enable docker`).
4. Adición del usuario `ec2-user` al grupo `docker`.

---

## 4. Verificación de Conectividad y ECR

Una vez levantada la instancia, las verificaciones estándar de la Issue #23 son:
* **Docker operativo:** `docker --version` y `docker ps` responden correctamente sin errores.
* **Autenticación ECR:** `aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.eu-west-1.amazonaws.com` autentica la instancia frente al repositorio privado.
* **Resolución RDS:** La instancia resuelve el endpoint privado de la base de datos RDS PostgreSQL (puerto `5432`), garantizando el flujo seguro EC2 → RDS.
