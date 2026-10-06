# Informe de Validación Real del MVP — 3D Cost Manager (Issue #34)

> **Estado:** COMPLETADO  
> **Resultado del Quality Gate:** **PASS WITH KNOWN LIMITATIONS**  
> **Versión Validada:** `v1.0.0`

---

## 1. Contexto y Entorno de Pruebas

Este informe documenta la validación funcional de extremo a extremo (E2E) de **3D Cost Manager v1.0.0**, contrastando el comportamiento real del backend Spring Boot, la base de datos PostgreSQL con Flyway, y la aplicación cliente Android (Jetpack Compose).

### Entorno Utilizado:
* **Sistema Operativo:** Windows 11 / Linux (para contenedores)
* **Runtime Backend:** Java 21 (Eclipse Temurin), Spring Boot 3.2.5
* **Base de Datos:** PostgreSQL 16 (vía Docker Compose)
* **Cliente Android:** Kotlin, Jetpack Compose, emulador Android (con IP loopback `10.0.2.2`) y soporte para dispositivo físico (mediante IP de red local LAN).

---

## 2. Tabla de Resultados de Pruebas (Checklist E2E)

| Prueba / Módulo | Resultado | Observaciones / Evidencia |
| :--- | :--- | :--- |
| **Backend Startup** | **PASS** | Arranque correcto de Spring Boot con perfil por defecto y conexión a PostgreSQL. |
| **PostgreSQL** | **PASS** | Contenedor Docker operativo en puerto `5432`. |
| **Flyway Migrations** | **PASS** | Aplicación correcta y secuencial de las migraciones `V1` a `V5`. |
| **Health Endpoint (`/api/v1/health`)** | **PASS** | Devuelve HTTP `200` con `{"status":"UP"}`. |
| **Android Build** | **PASS** | Compilación limpia de la app Android con Jetpack Compose y Retrofit. |
| **Instalación Android** | **PASS** | Ejecución correcta en emulador. |
| **Autenticación (Login / Registro)** | **PASS** | Emisión correcta de JWT y persistencia de sesión con `TokenManager`. |
| **Projects (CRUD)** | **PASS** | Creación, listado, consulta y archivado de proyectos operativos. |
| **Machines (Catálogo)** | **PASS** | Gestión completa de máquinas de impresión 3D. |
| **Materials (Catálogo)** | **PASS** | Gestión completa de materiales y unidades (`G`, `ML`, `UNIT`, etc.). |
| **Tools (Herramientas)** | **PASS** | Módulo completo de herramientas integrado en la UI y API REST. |
| **Cálculo de Costes** | **PASS** | Ejecución backend-driven mediante `CostCalculator` y `CostCalculationInput`. |
| **Creación de Quote** | **PASS** | Emisión correcta del presupuesto con snapshots inmutables. |
| **Consulta de Quote** | **PASS** | Recuperación y desglose financiero de presupuestos emitidos. |
| **Persistencia Completa** | **PASS** | Datos de proyectos, recursos y quotes persisten correctamente en PostgreSQL. |
| **Dispositivo Físico** | **PASS (Limitación de red)** | Requiere apuntar `BASE_URL` en `NetworkConfig.kt` a la IP de la LAN del host (ej. `192.168.1.X`) en lugar de `10.0.2.2`. |

---

## 3. Casos Negativos y Restricciones Verificadas

1. **Autenticación Inválida:** Peticiones sin token o con token alterado devuelven `401 Unauthorized`.
2. **Proyectos Archivados:** El dominio `Project` rechaza cualquier modificación o adición de recursos si el proyecto se encuentra en estado `ARCHIVED`, respondiendo con HTTP `409 Conflict`.
3. **Validación de Parámetros:** Los endpoints rechazan márgenes fuera de rango (`> 100` o `< 0`) y valores monetarios negativos con HTTP `400 Bad Request`.

---

## 4. Limitaciones Conocidas (Known Limitations)

1. **Configuración de Red en Dispositivo Físico:** El cliente Android utiliza por defecto `http://10.0.2.2:8080/`, que es el alias estándar del emulador para conectar con el localhost del host. Para probar en un teléfono físico, es necesario actualizar `BASE_URL` en `NetworkConfig.kt` a la IP local de la máquina de desarrollo en la red Wi-Fi.

---

## 5. Decisión Final

> **DECISIÓN: PASS WITH KNOWN LIMITATIONS**

El flujo funcional de **3D Cost Manager v1.0.0** opera correctamente de principio a fin, validando la arquitectura backend-driven, la inmutabilidad de presupuestos, la persistencia relacional y la interfaz nativa en Android.
