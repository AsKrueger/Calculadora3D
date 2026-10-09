# Roadmap de Saneamiento Técnico (Cleanup Roadmap)

**Proyecto:** Calculadora3D (3D Cost Manager)  
**Asociado a:** Issue #36 (Auditoría Exhaustiva)  
**Fecha:** 9 de Octubre de 2026

---

## 1. Visión General y Estrategia

Este documento establece la secuencia cronológica y ordenada de ejecuciones para el saneamiento de la base de código del repositorio **Calculadora3D**. 

Cada paso está aislado en una Issue independiente para garantizar:
- Trazabilidad atómica de cada cambio.
- Ninguna interrupción del funcionamiento del MVP.
- Verificación mediante batería de pruebas antes y después de cada integración.

---

## 2. Mapa de Dependencias entre Issues

```
[Issue #36] Auditoría y Diagnóstico (COMPLETADO)
       │
       ▼
[Issue #37] Saneamiento de Basura, Artefactos y .gitignore
       │
       ▼
[Issue #38] Saneamiento y Limpieza en Cliente Android (Warnings & BaseURL)
       │
       ▼
[Issue #39] Saneamiento de Seguridad y Configuración Backend (Spring/Flyway)
       │
       ▼
[Issue #40] Estandarización de Excepciones y Respuestas de Error
       │
       ▼
[Issue #41] Actualización y Alineación de Documentación Técnica
```

---

## 3. Desglose Detallado de Issues Posteriores

### Issue #37 — Limpieza de Artefactos Temporales, Basura y Reglas `.gitignore`
- **Objetivo:** Liberar espacio en el repositorio y prevenir la inclusión accidental de archivos generados.
- **Entregables:**
  - Eliminación de `android/java_pid21532.hprof` (800 MB).
  - Eliminación de archivos de registros temporales en `backend/` (`server.log`, `*.log`, `*_log.txt`).
  - Eliminación de cachés de IDE local en `android/.idea/caches/deviceStreaming.xml`.
  - Actualización de `.gitignore` para bloquear `*.hprof`, `*.log` y cachés.
- **Dependencias:** Ninguna (Primera tarea de limpieza).
- **Criterio de Aceptación:** `git status` limpio sin archivos basura rastreados.

---

### Issue #38 — Saneamiento de Advertencias de Compilación y Configuración en Android
- **Objetivo:** Eliminar warnings de compilación Kotlin y desacoplar la URL base de la red.
- **Entregables:**
  - Simplificación de las condiciones redundantes en `MachineFormScreen.kt`, `MaterialFormScreen.kt`, `ProjectFormScreen.kt` y `ToolFormScreen.kt`.
  - Refactorización de `NetworkConfig.kt` para utilizar `BuildConfig.BASE_URL` configurable desde Gradle.
  - Verificación de la navegación y formularios.
- **Dependencias:** Issue #37.
- **Criterio de Aceptación:** Build de Gradle en verde con 0 advertencias Kotlin en los formularios modificados.

---

### Issue #39 — Saneamiento de Seguridad y Configuración Backend
- **Objetivo:** Forzar buenas prácticas de seguridad y actualizar dependencias de base de datos.
- **Entregables:**
  - Configuración de `${JWT_SECRET}` en `application.properties` obligando el uso de variable de entorno en producción.
  - Eliminación de la propiedad obsoleta `hibernate.dialect` que genera advertencias HHH90000025.
  - Actualización de la versión de `flyway-core` en `pom.xml` para soporte nativo completo de PostgreSQL 16.
- **Dependencias:** Issue #37.
- **Criterio de Aceptación:** `mvn test` en verde con todas las 58 pruebas pasando.

---

### Issue #40 — Estandarización de Excepciones y Respuestas de Error Backend
- **Objetivo:** Homogeneizar las respuestas de error HTTP entre el backend y el frontend.
- **Entregables:**
  - Refactorización de `GlobalExceptionHandler.java` utilizando una estructura unificada `ApiErrorResponse`.
  - Verificación de los contratos de error en `AuthController`, `ProjectController`, `QuoteController` y catálogos.
- **Dependencias:** Issue #39.
- **Criterio de Aceptación:** Pruebas de integración de controladores pasando sin regresiones.

---

### Issue #41 — Actualización y Alineación de Documentación Técnica
- **Objetivo:** Reflejar el estado real del sistema libre de deuda técnica en la documentación.
- **Entregables:**
  - Actualización del `README.md` principal y guías de desarrollo en `docs/`.
  - Sincronización de ejemplos de configuración `.env.example`.
- **Dependencias:** Issues #37, #38, #39, #40.
- **Criterio de Aceptación:** Documentación verificado sin referencias a elementos eliminados o configuraciones obsoletas.

---

## 4. Quality Gates & Definition of Done para la Secuencia

Cada Issue individual se dará por concluida **únicamente** cuando cumpla el siguiente Quality Gate:

1. **Sin regresiones en pruebas:**
   - Backend: `mvn test` devuelve `BUILD SUCCESS` (58/58 pruebas).
   - Android: `gradlew test assembleDebug` devuelve `BUILD SUCCESSFUL`.
2. **Revisión de Seguridad:**
   - Ningún secreto ni credencial expuesta en archivos versionados.
3. **Control de Cambios:**
   - Commits atómicos alineados exclusivamente al alcance de la Issue correspondiente.
