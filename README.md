# 3D Cost Manager (v1.0.0)

> Aplicación integral para la **gestión de proyectos de impresión 3D y estimación automatizada de costes**, estructurada como un backend profesional en Java 21 / Spring Boot 3.2.5 y un cliente móvil nativo en Android (Jetpack Compose), con soporte de infraestructura cloud en AWS (CloudFormation y CI/CD automatizado).

---

## 🎯 Resumen del Proyecto

**3D Cost Manager** resuelve el problema de calcular con precisión los costes de fabricación de piezas 3D y post-procesado. A partir de catálogos de recursos (materiales, máquinas, herramientas) y parámetros económicos (mano de obra, márgenes, consumo eléctrico con tarifas reales integradas desde la API de ESIOS), el sistema calcula el coste base, ajustado y el precio final de venta, generando y persistiendo presupuestos inmutables (**Quotes**).

---

## 🏗️ Arquitectura del Sistema

### 1. Backend (Spring Boot 3.2.5 / Java 21)
* **Domain-Driven Design (DDD) Táctico:** Encapsulación de agregados (`Project`, `Quote`) para proteger invariantes de negocio (ej. prohibición de modificar proyectos archivados, inmutabilidad de snapshots financieros en presupuestos emitidos).
* **Motor de Cálculo Puro (`CostCalculator`):** Desacoplado del ORM mediante `CostCalculationInput`, garantizando cálculos deterministas con alta precisión (`BigDecimal`, escala 8 interna, escala 4 final, `HALF_UP`).
* **Contrato Estándar de Errores (`ApiError` & `GlobalExceptionHandler`):** Respuestas de error REST unificadas con códigos HTTP semánticos (`400`, `401`, `403`, `404`, `405`, `409`, `500`, `503`), desglose de validaciones por campo y cero filtrado de trazas o detalles internos.
* **Persistencia Robusta:** PostgreSQL 16 con migraciones versionadas gestionadas por **Flyway** e Hibernate en modo estricto de validación (`spring.jpa.hibernate.ddl-auto=validate`).
* **Seguridad Stateless:** Autenticación mediante JSON Web Tokens (JWT) inyectados desde `${JWT_SECRET}` con validación de arranque (`@PostConstruct`), cifrado BCrypt y gestión segura de secretos mediante **AWS Systems Manager Parameter Store**.

### 2. Cliente Android (Jetpack Compose & MVVM)
* **Arquitectura Reactiva:** MVVM estructurado con Jetpack Compose, flujos reactivos (`StateFlow`), y gestión robusta de estados de red (`UiState`, `NetworkError`).
* **Configuración Centralizada de Red (`BuildConfig.BASE_URL`):** Retrofit consume `BuildConfig.BASE_URL` configurado dinámicamente desde Gradle para soportar emuladores (`10.0.2.2:8080`), dispositivos físicos en LAN Wi-Fi (`192.168.X.X:8080`) o entornos de producción HTTPS.
* **Seguridad Local:** Interceptor de autenticación (`AuthInterceptor`) para la inyección automática de tokens y persistencia local segura de sesiones en Android Keystore con `EncryptedSharedPreferences` (AES-256).
* **Flujo MVP Completo:** Autenticación (Login/Registro), gestión de Proyectos, Materiales, Máquinas, Herramientas, Motor de Cálculo y Consulta/Generación de Presupuestos (`Quotes`).

### 3. Infraestructura Cloud AWS & DevOps
* **Infraestructura como Código (CloudFormation):**
  * `vpc-networking.yaml`: VPC dedicada con Subnet Pública (EC2) y Subnets Privadas (RDS).
  * `rds-postgresql.yaml`: Base de datos administrada Amazon RDS PostgreSQL 16 (`db.t4g.micro`) en subred privada.
  * `ec2.yaml`: Instancia de cómputo Amazon EC2 (`t4g.micro` ARM64 Graviton) con Docker y rol IAM de mínimo privilegio.
  * `parameter-store.yaml`: Gestión segura de configuración y credenciales (`SecureString`).
* **CI/CD Automatizado (GitHub Actions):** Pipeline que compila, valida tests, construye imágenes Docker optimizadas para ARM64 (`linux/arm64`), las publica en Amazon ECR, y realiza el despliegue automático en EC2 mediante AWS Systems Manager (SSM) Run Command con Health Check integrado (`/api/v1/health` → `UP`).
* **Nota de Estado:** Las plantillas de infraestructura y la canalización CI/CD están definidas y validadas en código fuente. Las verificaciones en vivo en la nube AWS se marcan como `NO VERIFICADO EN VIVO` hasta el aprovisionamiento formal de la cuenta AWS de producción.

---

## 🛠️ Tecnologías Principales

* **Backend:** Java 21, Spring Boot 3.2.5, Spring Data JPA / Hibernate, Flyway, Spring Security, JJWT, PostgreSQL 16, JUnit 5, Mockito, Testcontainers.
* **Android:** Kotlin 2.1.0, Jetpack Compose, Material 3, Retrofit, Kotlinx Serialization, Coroutines, Gradle 8.9, JDK 21 (`jbr-21`).
* **DevOps / Cloud:** Docker & Docker Compose, Amazon EC2, Amazon RDS, Amazon ECR, AWS SSM Parameter Store, GitHub Actions (OIDC).

---

## 🐳 Ejecución Local y Pruebas

### 1. Backend (Spring Boot + PostgreSQL con Testcontainers / Docker Compose)
Para compilar y ejecutar las 69 pruebas unitarias y de integración del backend:
```bash
cd backend
mvn test
```
Para levantar el stack local con Docker Compose:
```bash
cp .env.example .env
docker compose up --build -d
curl http://localhost:8080/api/v1/health
```

### 2. Cliente Android (Gradle Wrapper)
Para compilar y verificar el cliente Android (Debug y Release):
```bash
cd android
./gradlew test assembleDebug assembleRelease
```
Ubicación de artefactos generados:
- **Debug APK:** `android/app/build/outputs/apk/debug/app-debug.apk`
- **Release APK:** `android/app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📌 Estado del Saneamiento Técnico (Issues #36–#41)

- [x] **Issue #36:** Auditoría exhaustiva de la base de código.
- [x] **Issue #37:** Eliminación de artefactos temporales (~800MB heap dump y logs) e inyección de reglas `.gitignore`.
- [x] **Issue #38:** Configuración centralizada `BuildConfig.BASE_URL` y saneamiento de advertencias Kotlin en formularios Compose.
- [x] **Issue #39:** Inyección obligatoria de `${JWT_SECRET}` con validación de arranque y eliminación de advertencia HHH90000025 en Hibernate.
- [x] **Issue #40:** Estandarización del contrato de errores REST (`ApiError` / `GlobalExceptionHandler` con suite de pruebas unitarias dedicada).
- [x] **Issue #41:** Alineación y actualización completa de la documentación técnica.
