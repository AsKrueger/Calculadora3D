# 3D Cost Manager (v1.0.0)

> Aplicación integral para la **gestión de proyectos de impresión 3D y estimación automatizada de costes**, estructurada como un backend profesional en Java / Spring Boot y un cliente móvil nativo en Android (Jetpack Compose), con soporte de infraestructura cloud en AWS (CloudFormation y CI/CD automatizado).

---

## 🎯 Resumen del Proyecto

**3D Cost Manager** resuelve el problema de calcular con precisión los costes de fabricación de piezas 3D y post-procesado. A partir de catálogos de recursos (materiales, máquinas, herramientas) y parámetros económicos (mano de obra, márgenes, consumo eléctrico con tarifas reales integradas desde la API de ESIOS), el sistema calcula el coste base, ajustado y el precio final de venta, generando y persistiendo presupuestos inmutables (**Quotes**).

---

## 🏗️ Arquitectura del Sistema

### 1. Backend (Spring Boot 3 / Java 21)
* **Domain-Driven Design (DDD) Táctico:** Encapsulación de agregados (`Project`, `Quote`) para proteger invariantes de negocio (ej. prohibición de modificar proyectos archivados, inmutabilidad de snapshots financieros en presupuestos emitidos).
* **Motor de Cálculo Puro (`CostCalculator`):** Desacoplado del ORM mediante `CostCalculationInput`, garantizando cálculos deterministas con alta precisión (`BigDecimal`, escala 8 interna, escala 4 final, `HALF_UP`).
* **Persistencia Robusta:** PostgreSQL 16 con migraciones versionadas gestionadas por **Flyway** e Hibernate en modo estricto de validación (`spring.jpa.hibernate.ddl-auto=validate`).
* **Seguridad Stateless:** Autenticación mediante JSON Web Tokens (JWT), cifrado BCrypt y gestión segura de secretos mediante **AWS Systems Manager Parameter Store**.

### 2. Cliente Android (Jetpack Compose & MVVM)
* **Arquitectura Reactiva:** MVVM estructurado con Jetpack Compose, flujos reactivos (`StateFlow`), y gestión robusta de estados de red (`UiState`, `NetworkError`).
* **Consumo REST:** Retrofit configurado con un interceptor de autenticación (`AuthInterceptor`) para la inyección automática de tokens y persistencia local segura de sesiones (`TokenManager`).
* **Flujo MVP Completo:** Autenticación (Login/Registro), gestión de Proyectos, Materiales, Máquinas, Herramientas, Motor de Cálculo y Consulta/Generación de Presupuestos (`Quotes`).

### 3. Infraestructura Cloud AWS & DevOps
* **Infraestructura como Código (CloudFormation):**
  * `vpc-networking.yaml`: VPC dedicada con Subnet Pública (EC2) y Subnets Privadas (RDS).
  * `rds-postgresql.yaml`: Base de datos administrada Amazon RDS PostgreSQL 16 (`db.t4g.micro`) en subred privada.
  * `ec2.yaml`: Instancia de cómputo Amazon EC2 (`t4g.micro` ARM64 Graviton) con Docker y rol IAM de mínimo privilegio.
  * `parameter-store.yaml`: Gestión segura de configuración y credenciales (`SecureString`).
* **CI/CD Automatizado (GitHub Actions):** Pipeline que compila, valida tests, construye imágenes Docker optimizadas para ARM64 (`linux/arm64`), las publica en Amazon ECR, y realiza el despliegue automático en EC2 mediante AWS Systems Manager (SSM) Run Command con Health Check integrado (`/api/v1/health` → `UP`).

---

## 🛠️ Tecnologías Principales

* **Backend:** Java 21, Spring Boot 3.2.5, Spring Data JPA / Hibernate, Flyway, Spring Security, JJWT, PostgreSQL 16, JUnit 5, Mockito, Testcontainers.
* **Android:** Kotlin, Jetpack Compose, Material 3, Retrofit, Kotlinx Serialization, Coroutines.
* **DevOps / Cloud:** Docker & Docker Compose, Amazon EC2, Amazon RDS, Amazon ECR, AWS SSM Parameter Store, GitHub Actions (OIDC).

---

## 🐳 Ejecución Local con Docker Compose

1. **Configurar variables de entorno:**
   ```bash
   cp .env.example .env
   ```
2. **Levantar el stack (Backend + PostgreSQL):**
   ```bash
   docker compose up --build -d
   ```
3. **Verificar el estado del health check:**
   ```bash
   curl http://localhost:8080/api/v1/health
   ```

---

## 🧪 Testing y Verificación

Para ejecutar la suite de pruebas unitarias y de dominio puros:
```bash
cd backend
./mvnw clean verify
```

---

## 📌 Estado de la Release

> **Versión:** `v1.0.0` (MVP Funcional Completado y Verificado mediante Quality Gate **PASS**).
