# Auditoría Técnica Avanzada Global — 3D Cost Manager

> **Estado:** COMPLETADO (Actualizado tras Issues #36–#41)  
> **Alcance:** Análisis exhaustivo de código fuente y arquitectura de todo el proyecto (Backend Spring Boot, Cliente Android Jetpack Compose, Persistencia Flyway/PostgreSQL, Contenedorización Docker e Infraestructura Cloud AWS).

---

## 1. Executive Summary

El proyecto **3D Cost Manager** ha sido auditado y saneado de extremo a extremo a nivel de código fuente, configuración, esquemas de base de datos, infraestructura CloudFormation y aplicación cliente Android.

Tras la ejecución del plan de saneamiento técnico (Issues #36 a #41):
1. **Limpieza de Artefactos:** Se eliminaron del entorno de trabajo más de 800 MB de basura (heap dumps `.hprof` y logs temporales), garantizando un repositorio ligero con reglas `.gitignore` actualizadas.
2. **Saneamiento de Advertencias y Configuración:** Se eliminaron las 8 advertencias Kotlin en formularios Compose y se migró la URL base de la red a `BuildConfig.BASE_URL` configurable desde Gradle.
3. **Seguridad y Errores REST:** Se forzó la validación de `${JWT_SECRET}` en el arranque del backend y se unificó la estructura de errores REST bajo `ApiError` con 69 pruebas del backend pasando en verde (`BUILD SUCCESS`).
4. **Estado de Compilación:** Compilación Android Debug/Release y suite de pruebas backend en verde.

El proyecto se encuentra **100% preparado para la validación del flujo funcional en emulador y pruebas físicas de instalación de la APK (Issue #34)**.

---

## 2. Auditoría del Backend (Spring Boot / Java 21)

### 2.1 Capa de Aplicación (`application.controller`, `application.service`, `application.dto`)
* **Controladores REST:** Limpios, bien anotados con `@RestController`, mapeados bajo `/api/v1/` (`AuthController`, `ProjectController`, `MachineController`, `MaterialController`, `QuoteController`, `HealthController`). Utilizan `@Valid` para validación automática de beans y DTOs inmutables basados en `record` (`ProjectCreateRequest`, `QuoteCreateRequest`, etc.).
* **Manejo de Errores Global (`GlobalExceptionHandler` & `ApiError`):** Traduce excepciones de dominio e infraestructura (`EntityNotFoundException`, `IllegalArgumentException`, `IllegalStateException`, `DataIntegrityViolationException`, `BadCredentialsException`, `AccessDeniedException`, validaciones por campo) en respuestas HTTP normalizadas (`ApiError`) con códigos de estado semánticos (`400`, `401`, `403`, `404`, `405`, `409`, `500`, `503`).
* **Servicios Transaccionales:** `ProjectService`, `QuoteService`, `MachineService`, `MaterialService` y `AuthService` gestionan la coordinación transaccional (`@Transactional`, `@Transactional(readOnly = true)`) delegando con éxito las reglas de negocio en los agregados de dominio.

### 2.2 Dominio y Motor de Cálculo (`domain.model`, `domain.calculation`)
* **Agregado `Project`:** Protege sus invariantes. Rechaza modificaciones si el estado es `ARCHIVED`, valida valores no negativos en mano de obra (`laborHours`, `laborCostPerHour`) y expone las colecciones de asociación como vistas inmodificables (`Collections.unmodifiableList`).
* **Agregado `Quote`:** Representa un **snapshot financiero histórico inmutable** tras su creación mediante el método factory `Quote.create(...)`. Sus campos económicos (`baseCost`, `adjustedCost`, `finalPrice`, `marginPercentage`, `safetyPercentage`, `project`) no tienen setters públicos, preservando la integridad histórica frente a cambios posteriores en el proyecto asociado.
* **Motor de Cálculo (`CostCalculator`):** Completamente desacoplado del ORM mediante el modelo de entrada puro `CostCalculationInput`. Opera con alta precisión (`BigDecimal`, escala interna de 8 decimales, final de 4, redondeo `HALF_UP`) y es 100% testeable sin levantar Spring ni base de datos.
* **Proveedor Eléctrico (`EsiosElectricityProvider`):** Implementa el puerto de salida `ElectricityPriceProvider` utilizando Spring `RestClient` para consultar precios horarios en la API de ESIOS con manejo seguro de tokens y respuestas.

### 2.3 Persistencia y Base de Datos (`domain.repository`, `src/main/resources/db/migration`)
* **Esquema Relacional:** Migraciones versionadas gestionadas por Flyway (`V1__Initial_Setup.sql` a `V5__Create_Users_Table.sql`).
* **Precisión Numérica:** Uso riguroso de `NUMERIC(19,4)` para costes, precios y cantidades, y `NUMERIC(5,2)` para porcentajes, evitando problemas de redondeo en coma flotante.
* **Integridad y ORM:** Relaciones con `@ManyToOne(fetch = FetchType.LAZY)`. Hibernate configurado estrictamente en modo validación (`spring.jpa.hibernate.ddl-auto=validate`), deduciendo automáticamente el dialecto de PostgreSQL 16 sin advertencias.

### 2.4 Seguridad (`infrastructure.security`)
* **Autenticación Stateless:** Filtro JWT (`JwtAuthenticationFilter`) que extrae y valida tokens Bearer, integrándose con Spring Security. Inyección obligatoria de `${JWT_SECRET}` validada mediante `@PostConstruct` en `JwtService.java`.
* **Cifrado:** Uso de BCrypt para el almacenamiento seguro de contraseñas de usuario.
* **Manejo de Accesos:** `DelegatedAuthenticationEntryPoint` y `DelegatedAccessDeniedHandler` para respuestas 401 y 403 controladas.

---

## 3. Auditoría del Cliente Android (`com.tdcostmanager.app`)

### 3.1 Arquitectura MVVM y UI (`ui/`, `domain/`, `data/`)
* **Interfaz Declarativa:** Desarrollada completamente con **Jetpack Compose**, estructurada en pantallas reutilizables (`LoginScreen`, `RegisterScreen`, `ProjectListScreen`, `ProjectDetailScreen`, `MachineListScreen`, `MaterialListScreen`, etc.) y navegada mediante `NavGraph`.
* **Gestión de Estado:** ViewModels reactivos (`ProjectViewModel`, `MachineViewModel`, `MaterialViewModel`, `AuthViewModel`) que exponen estados inmutables mediante `StateFlow` y gestionan errores de red con `UiState` y `NetworkError`.
* **Configuración de Red Centralizada:** Retrofit configurado en `NetworkConfig` consumiendo `BuildConfig.BASE_URL` inyectado desde Gradle, facilitando la conexión en emulador (`10.0.2.2:8080`), LAN física o producción HTTPS.
* **Seguridad Local:** Persistencia segura de JWT mediante `TokenManager` con `EncryptedSharedPreferences` (AES-256) en la Keystore de Android.

---

## 4. Auditoría de Contenedorización e Infraestructura AWS

### 4.1 Docker (`backend/Dockerfile`, `docker-compose.yml`)
* **Multi-stage Build:** Compilación Maven seguida de empaquetado en JRE ligero con usuario no root.
* **Soporte Multi-arquitectura:** Optimizado para arquitecturas ARM64 (`linux/arm64` para instancias EC2 `t4g.micro` Graviton) y x86_64.
* **Orquestación Local:** `docker-compose.yml` levanta simultáneamente la aplicación Spring Boot y PostgreSQL con persistencia en volumen.

### 4.2 CloudFormation AWS (`infra/aws/`)
* **Networking (`vpc-networking.yaml`):** VPC aislada con Subnet Pública (EC2) y Subnets Privadas (RDS).
* **Base de Datos (`rds-postgresql.yaml`):** Instancia RDS PostgreSQL 16 (`db.t4g.micro`) en subred privada, sin acceso público (`PubliclyAccessible: false`).
* **Cómputo (`ec2.yaml`):** Instancia EC2 (`t4g.micro`) con script de arranque en *User Data* para instalación de Docker.
* **Configuración (`parameter-store.yaml`):** Definición de parámetros bajo `/3d-cost-manager/` separando texto plano y `SecureString` con cifrado KMS.
* **Estado de Verificación:** Las plantillas de infraestructura están totalmente definidas en código fuente (`NO VERIFICADO EN VIVO` en AWS real hasta aprovisionamiento formal).

---

## 5. Auditoría de Testing

* **Suite de Pruebas Backend:**
  - **Pruebas Unitarias e Integradas:** 69 pruebas ejecutadas con `BUILD SUCCESS` (68 pasaron, 0 fallos, 0 errores, 1 omitida por requerir token ESIOS real).
  - Incluye `GlobalExceptionHandlerTest` (11 pruebas para el contrato de errores REST).
* **Compilación Android:**
  - `./gradlew test assembleDebug assembleRelease` finalizado con **`BUILD SUCCESSFUL`** y 0 advertencias de compilación Kotlin.

---

## 6. Saneamiento Técnico Completado (Issues #36–#41)

| Issue | Nombre / Alcance | Estado | Resultado |
|---|---|---|---|
| **#36** | Auditoría Exhaustiva del Repositorio | Completado | Informe `REPOSITORY_AUDIT.md` y `CLEANUP_ROADMAP.md` |
| **#37** | Limpieza de Basura y `.gitignore` | Completado | ~800MB heap dump y logs eliminados, `.gitignore` actualizado |
| **#38** | Configuración `BuildConfig.BASE_URL` y Warnings Kotlin | Completado | URL base configurable, 8 warnings de formularios resueltos |
| **#39** | Seguridad Backend y Flyway/Hibernate | Completado | Inyección `${JWT_SECRET}` validada, warning HHH90000025 eliminado |
| **#40** | Contrato de Errores REST (`ApiError`) | Completado | `GlobalExceptionHandler` refactorizado + `GlobalExceptionHandlerTest` (11 tests) |
| **#41** | Alineación de Documentación Técnica | Completado | `README.md`, `ANDROID_BUILD_GUIDE.md`, `API_ERROR_CONTRACT.md` sincronizados |

---

## 7. Próximo Paso (Regreso al Roadmap Funcional)

- **Siguiente Hito:** **Issue #34 — Verificación del Flujo Funcional en Emulador y Generación/Instalación del APK en Teléfono Físico**.
