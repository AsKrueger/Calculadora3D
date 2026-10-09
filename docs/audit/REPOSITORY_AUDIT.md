# Informe de Auditoría Exhaustiva del Repositorio (Issue #36)

**Proyecto:** Calculadora3D (3D Cost Manager)  
**Fecha de Auditoría:** 9 de Octubre de 2026  
**Estado:** Completado (Documentado sin eliminación de código)  
**Autor:** Senior Android & Backend System Architect

---

## 1. Resumen Ejecutivo

Se ha llevado a cabo una auditoría técnica integral sobre la totalidad de la base de código del proyecto **Calculadora3D**, abarcando la aplicación móvil Android (Kotlin / Jetpack Compose), el servidor backend (Spring Boot 3 / Java 21), la infraestructura de despliegue en la nube (AWS / Docker), las canalizaciones CI/CD (GitHub Actions) y la documentación técnica asociada.

### Hallazgos Clave
1. **Basura y artefactos temporales (800 MB+):** Presencia de un archivo heap dump (`android/java_pid21532.hprof`) de 800 MB y múltiples archivos de log temporales (`*.log`, `*.txt`) acumulados en la raíz del proyecto backend.
2. **Estabilidad de Compilación:** La base de código compila y ejecuta pruebas unitarias e integradas con éxito (58 pruebas en backend sin fallos, compilación Android Debug y Release en verde).
3. **Calidad de Código e Incoherencias:** Múltiples variables hardcodeadas (ej. URL base `10.0.2.2:8080`), advertencias de compilación por expresiones redundantes en formularios Compose, e importaciones de librerías no utilizadas.
4. **Seguridad y Secretos:** Clave secreta JWT genérica configurada en `application.properties` para entornos locales y ausencia de HTTPS en las llamadas del cliente móvil hacia la API.

---

## 2. Alcance Real de la Inspección

A continuación se detalla el perímetro inspeccionado en el repositorio:

- **Cliente Android (`/android`):**
  - Configuración Gradle (`build.gradle.kts`, `gradle.properties`, wrapper Gradle 8.9).
  - Capa de interfaz de usuario Compose (`ui/auth`, `ui/machine`, `ui/material`, `ui/project`, `ui/quote`, `ui/tool`, `ui/health`).
  - Capa de datos y red (`data/remote/api`, `data/remote/dto`, `data/repository`, `data/local/TokenManager`).
  - Navegación y factorías de ViewModel (`NavGraph.kt`, `ViewModelFactory.kt`).
- **Servidor Backend (`/backend`):**
  - Controladores REST y servicios (`application/controller`, `application/service`).
  - Modelo de dominio y lógica de cálculo determinista (`domain/model`, `domain/calculation/CostCalculator`).
  - Seguridad JWT y filtros OkHttp/Spring (`infrastructure/security`).
  - Integración eléctrica ESIOS (`infrastructure/electricity`).
  - Migraciones de base de datos Flyway (`resources/db/migration/V1__Initial_Setup.sql` a `V5__Create_Users_Table.sql`).
- **Infraestructura y CI/CD (`/infra`, `/.github`, raíz):**
  - Plantillas CloudFormation AWS (`infra/aws/*.yaml`).
  - Configuración Docker y Compose (`Dockerfile`, `docker-compose.yml`).
  - Workflows de integración continua (`.github/workflows/backend-ci-cd.yml`).
- **Documentación (`/docs`, `README.md`):**
  - Documentos de arquitectura, guías de instalación y auditorías de MVP.

---

## 3. Estructura Actual del Proyecto

```
Calculadora3D/
├── .env / .env.example
├── docker-compose.yml
├── README.md
├── .github/
│   └── workflows/
│       └── backend-ci-cd.yml
├── android/
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/tdcostmanager/app/
│   │       ├── core/
│   │       ├── data/
│   │       ├── domain/
│   │       └── ui/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── gradle.properties
│   └── java_pid21532.hprof  <-- [ARCHIVADO COMO BASURA: 800MB]
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── server.log / *.log   <-- [ARCHIVADO COMO BASURA: Logs temporales]
│   └── src/
│       ├── main/java/com/tdcostmanager/backend/
│       └── main/resources/db/migration/
├── docs/
│   ├── ANDROID_BUILD_GUIDE.md
│   ├── AUDIT_GLOBAL.md
│   ├── MVP_SCOPE_AUDIT.md
│   ├── MVP_VALIDATION_REPORT.md
│   ├── QUALITY_GATE_MVP.md
│   └── aws/
└── infra/
    └── aws/
```

---

## 4. Inventario de Hallazgos

A continuación se registra el inventario completo clasificado según la taxonomía obligatoria (A-H) con la ficha técnica de 8 campos:

### Categoría A: Código Muerto y No Utilizado
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **A-01** | `android/app/src/main/java/com/tdcostmanager/app/ui/health/HealthScreen.kt` | Pantalla de estado del servidor sin uso final por el usuario | Código legacy de diagnóstico de desarrollo no integrado en el flujo principal | Bajo | Conservar o mover a sección de diagnósticos de desarrollador | `HealthViewModel`, `HealthApi` | Compilación `./gradlew assembleDebug` |
| **A-02** | `android/app/src/main/java/com/tdcostmanager/app/domain/model/UnitType.kt` | Enum de unidades sin referencias en ViewModels nuevos | Definición estática no vinculada a los conversores del backend | Bajo | Simplificar y alinear con el backend | DTOs de Materiales y Herramientas | `gradlew test` |

### Categoría B: Archivos Basura y Artefactos Temporales
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **B-01** | `android/java_pid21532.hprof` | Dump de memoria de 800 MB en la raíz del módulo Android | Ocupa espacio masivo en disco y ralentiza operaciones de Git | Crítico | Eliminar archivo del control de versiones e incluir `*.hprof` en `.gitignore` | Ninguna | `git status` |
| **B-02** | `backend/server.log`, `backend/*.log`, `backend/*.txt` | 8 archivos de registro y texto temporal en el directorio raíz del backend | Contaminan el directorio de código y pueden contener trazados de depuración | Medio | Eliminar archivos y añadir reglas a `.gitignore` | Ninguna | `git status` |
| **B-03** | `android/.idea/caches/deviceStreaming.xml` | Archivo de caché de IDE de 114 KB versionado | Configuración de usuario interna de Android Studio que no debe rastrearse | Bajo | Eliminar del índice Git y asegurar `.gitignore` | Configuración IDE local | Abrir proyecto en Android Studio |

### Categoría C: Duplicidad e Incoherencia
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **C-01** | `android/app/src/main/java/com/tdcostmanager/app/ui/common/UiState.kt` | Definición genérica de `UiState` coexistiendo con `AuthUiState` | Duplicidad en la abstracción de estados UI | Bajo | Unificar bajo una jerarquía genérica reutilizable | Todos los ViewModels | `gradlew assembleDebug` |
| **C-02** | `backend/src/main/resources/application.properties` vs `application-aws.properties.example` | Duplicidad de propiedades con inconsistencias en dialectos JPA | `hibernate.dialect` declarado explícitamente causando advertencia HHH90000025 | Bajo | Eliminar propiedad obsoleta de dialecto de `application.properties` | Spring Boot JPA | `mvn test` |

### Categoría D: Estructura y Acoplamiento
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **D-01** | `android/app/src/main/java/com/tdcostmanager/app/data/remote/network/NetworkConfig.kt` | URL base estática `http://10.0.2.2:8080/` | Acoplamiento rígido con el emulador local. Falla en dispositivos físicos | Alto | Mover a `BuildConfig.BASE_URL` configurado desde Gradle | `build.gradle.kts` | Ejecutar app en emulador y dispositivo real |
| **D-02** | `backend/src/main/java/com/tdcostmanager/backend/application/controller/GlobalExceptionHandler.java` | Manejo de excepciones mezcla errores de validación con runtime genérico | Respuestas HTTP inconsistentes | Medio | Refactorizar para homogeneizar la estructura de respuesta de error | Controladores REST | `mvn test` |

### Categoría E: Dependencias
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **E-01** | `backend/pom.xml` | Advertencia de compatibilidad Flyway con PostgreSQL 16 | Flyway 9.22 muestra recomendación de actualización para soporte oficial PG16 | Bajo | Actualizar plugin y dependencia de Flyway a versión 10+ | Migraciones Flyway | `mvn test` |

### Categoría F: Calidad y Advertencias de Compilación
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **F-01** | `android/app/src/main/java/com/tdcostmanager/app/ui/machine/MachineFormScreen.kt` (líneas 39, 129) | Advertencia Kotlin: `Condition is always 'true'` | Expresiones condicionales redundantes en validación de formulario | Bajo | Simplificar la lógica lógica eliminando chequeos redundantemente verdaderos | Formulario de Máquinas | `gradlew compileReleaseKotlin` |
| **F-02** | `android/app/src/main/java/com/tdcostmanager/app/ui/material/MaterialFormScreen.kt` (líneas 42, 133) | Advertencia Kotlin: `Condition is always 'true'` | Expresiones condicionales redundantes | Bajo | Simplificar chequeos | Formulario de Materiales | `gradlew compileReleaseKotlin` |
| **F-03** | `android/app/src/main/java/com/tdcostmanager/app/ui/project/ProjectFormScreen.kt` & `ToolFormScreen.kt` | Advertencias Kotlin: `Condition is always 'true'` | Expresiones condicionales redundantes | Bajo | Simplificar chequeos | Formularios de Proyectos y Herramientas | `gradlew compileReleaseKotlin` |

### Categoría G: Seguridad
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **G-01** | `backend/src/main/resources/application.properties` | Clave secreta JWT por defecto expuesta en repositorio público/privado | Clave simétrica fija en código fuente. Riesgo de falsificación de tokens en entornos compartidos | Alto | Forzar uso de variable de entorno `${JWT_SECRET}` con fallback seguro | `JwtService.java` | `mvn test` |
| **G-02** | `android/app/src/main/java/com/tdcostmanager/app/data/remote/network/NetworkConfig.kt` | Protocolo HTTP no cifrado para la API REST | Tráfico expuesto a ataques de interceptación | Alto | Configurar esquema HTTPS para producción y `networkSecurityConfig` para desarrollo | Comunicación Cliente-Servidor | `gradlew assembleDebug` |

### Categoría H: Documentación
| ID | Archivo o componente | Evidencia | Problema | Riesgo | Acción propuesta | Dependencias | Validación |
|---|---|---|---|---|---|---|---|
| **H-01** | `docs/AUDIT_GLOBAL.md` & `README.md` | Referencias a características pasadas y elementos de UI ya eliminados | La documentación no refleja el estado actual de las pantallas de Auth sin botones fake | Bajo | Actualizar capturas y especificaciones de las pantallas Auth | Ninguna | Inspección visual |

---

## 5. Evidencias y Clasificación de Riesgos

### Distribución de Riesgos

```
  [CRÍTICO]  █ (1)  - Dump de memoria de 800MB (B-01)
  [ALTO]     ███ (3) - URL base estática (D-01), Secretos JWT (G-01), Tráfico HTTP (G-02)
  [MEDIO]    ██ (2)  - Archivos de Log (B-02), Excepciones Backend (D-02)
  [BAJO]     ██████ (6) - Resto de hallazgos (A-01, A-02, B-03, C-01, C-02, E-01, F-01..03, H-01)
```

---

## 6. Archivos Candidatos a Eliminación (Pendientes de Confirmación)

> **CUMPLIMIENTO DE REGLA DE SEGURIDAD #1:** Ninguno de los siguientes archivos ha sido eliminado durante esta Issue (#36). Quedan marcados formalmente para su eliminación coordinada en las Issues posteriores.

1. `android/java_pid21532.hprof` (800 MB)
2. `android/.idea/caches/deviceStreaming.xml`
3. `backend/server.log`
4. `backend/phase2_log.txt`
5. `backend/server_retry.log`
6. `backend/migration_log.txt`
7. `backend/issue4_final_log.txt`
8. `backend/phase1_issue4_log.txt`
9. `backend/phase3_verification.txt`
10. `backend/issue4_final_log_retry.txt`

---

## 7. Mejoras Estructurales Recomendadas

1. **Inyección de Dependencias en Android:**
   - Actualmente las instancias de API y Repositorios se construyen directamente en `ViewModelFactory.kt` y `NetworkConfig.kt`. Se recomienda introducir Hilt / Koin en fases avanzadas para desacoplar el ciclo de vida.
2. **Homogeneización de Manejo de Excepciones:**
   - Estandarizar la respuesta de errores en Spring Boot mediante un DTO `ApiErrorResponse(timestamp, status, error, message, path)`.
3. **Optimización de `.gitignore`:**
   - Incluir explícitamente patrones para preventivamente ignorar dumps de memoria (`*.hprof`), logs temporales (`*.log`, `*_log.txt`) y cachés de IDE.

---

## 8. Dependencias Candidatas a Eliminación o Actualización

- **Backend (Maven):**
  - Actualizar `flyway-core` de `9.22.3` a la rama `10.x` para soporte nativo completo de PostgreSQL 16 sin advertencias.
  - Asegurar versión estable de Jackson y Spring Boot Security 3.2.5.
- **Android (Gradle):**
  - Mantener Android Gradle Plugin `8.7.3` y Kotlin `2.1.0`.
  - Jetpack Compose BOM `2024.02.00` se encuentra estable.

---

## 9. Estado Inicial de Compilación y Pruebas

### Pruebas Backend (Spring Boot / Maven)
- **Comando:** `mvn test`
- **Resultado:** **ÉXITO (`BUILD SUCCESS`)**
- **Métricas:** 58 ejecuciones de prueba, 0 fallos, 0 errores, 1 omitida (prueba de integración ESIOS con token real).
- **Tiempo de ejecución:** 1 min 07 seg.

### Compilación Android (Gradle)
- **Comando:** `$env:ANDROID_PREFS_ROOT="" ; .\gradlew.bat test assembleDebug assembleRelease`
- **Resultado:** **ÉXITO (`BUILD SUCCESSFUL`)**
- **Estado:** 43 tareas ejecutadas sin errores. Advertencias de compilación Kotlin documentadas en los hallazgos F-01 a F-03.

---

## 10. Limitaciones de la Auditoría

- **Entorno de Despliegue AWS Real:** La verificación de plantillas CloudFormation en AWS se basa en análisis estático de las plantillas `.yaml`. No se realizaron despliegues reales en la cuenta AWS del cliente durante esta auditoría para evitar costes adicionales no programados (`NO VERIFICADO EN VIVO`).
- **Prueba Real ESIOS API:** La llamada real contra la API de ESIOS con token de producción fue omitida en las pruebas automáticas continuas mediante `@Disabled` controlado.

---

## 11. Plan Priorizado de Issues Posteriores

Para abordar de manera segura y metódica el saneamiento técnico sin introducir regresiones ni alterar el comportamiento del MVP, se propone el siguiente desglose de Issues:

1. **Issue #37 — Limpieza de Artefactos Temporales, Basura y Reglas `.gitignore`**
   - Eliminar `java_pid21532.hprof`, logs del backend y actualizar `.gitignore`.
2. **Issue #38 — Saneamiento de Advertencias de Compilación y Simplificación en Android**
   - Limpiar advertencias de condicionales en formularios Compose y refactorizar `NetworkConfig.kt` para usar `BuildConfig.BASE_URL`.
3. **Issue #39 — Saneamiento de Seguridad y Variables de Entorno en Backend**
   - Configurar `${JWT_SECRET}` obligatorio, remover dialecto de Hibernate obsoleto y actualizar Flyway.
4. **Issue #40 — Homogeneización de Respuestas de Error y Manejo de Excepciones**
   - Refactorizar `GlobalExceptionHandler.java` e igualar respuestas con el frontend.
5. **Issue #41 — Actualización y Sincronización de Documentación del Repositorio**
   - Actualizar `README.md`, diagramas y documentos en `docs/` con el estado final limpio.
