# Configuración de Secretos y Parámetros con AWS Parameter Store — 3D Cost Manager

Este documento detalla la estrategia de gestión de configuración y secretos para el backend de **3D Cost Manager** utilizando **AWS Systems Manager Parameter Store**.

---

## 1. Namespace y Estructura de Parámetros

Todos los parámetros de configuración del proyecto se agrupan bajo el namespace estricto:

```text
/3d-cost-manager/
```

### Parámetros Definidos:
* `/3d-cost-manager/database/url` (Tipo: `String`) — Endpoint JDBC de la base de datos RDS.
* `/3d-cost-manager/database/username` (Tipo: `String`) — Usuario administrador de la base de datos.
* `/3d-cost-manager/database/password` (Tipo: `SecureString`) — Contraseña de la base de datos.
* `/3d-cost-manager/jwt/secret` (Tipo: `SecureString`) — Clave secreta para la firma y validación de tokens JWT.
* `/3d-cost-manager/esios/token` (Tipo: `SecureString`) — Token de acceso a la API de ESIOS.

---

## 2. Tipos de Parámetros (`String` vs `SecureString`)

* **`String`:** Utilizado para datos no sensibles (URLs, nombres de usuario públicos, identificadores). No requieren cifrado adicional en reposo más allá del cifrado estándar de AWS Systems Manager.
* **`SecureString`:** Utilizado exclusivamente para información sensible y credenciales (contraseñas, secretos JWT, tokens de API). Estos valores se cifran de forma transparente utilizando claves gestionadas por KMS (`AWS/ssm`).

---

## 3. Seguridad e IAM (Mínimo Privilegio)

El rol de IAM asignado a la instancia EC2 (`3d-cost-manager-ec2-role`) limita los permisos de Parameter Store estrictamente al namespace del proyecto, impidiendo cualquier operación de escritura, modificación o lectura fuera de su alcance:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:eu-west-1:ACCOUNT_ID:parameter/3d-cost-manager/*"
    }
  ]
}
```

---

## 4. Integración con Spring Boot

El backend consume estos parámetros en tiempo de ejecución mediante el perfil `aws` y la plantilla de configuración `application-aws.properties.example`, donde las propiedades se vinculan a variables de entorno:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
jwt.secret=${JWT_SECRET}
esios.api.token=${ESIOS_API_TOKEN}
```

En la instancia EC2, antes de levantar el contenedor Docker, un script de inicio o el entorno de ejecución inyecta los valores recuperados de Parameter Store (desencriptando automáticamente los `SecureString` gracias al rol IAM de la instancia).

---

## 5. Aislamiento del Desarrollo Local

El flujo de desarrollo local permanece completamente intacto:
* **Entorno local:** Sigue utilizando `Docker Compose`, un archivo `.env` local y PostgreSQL local.
* **Entorno AWS:** Utiliza Parameter Store. Ningún secreto de producción se almacena nunca en el repositorio Git (`.gitignore` incluye archivos de entorno locales).
