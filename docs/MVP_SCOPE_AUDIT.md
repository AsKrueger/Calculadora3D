# Auditoría Funcional del Alcance MVP — 3D Cost Manager (Issue #29)

> **Estado:** COMPLETADO  
> **Objetivo:** Auditar el estado funcional real del proyecto (Backend Spring Boot + Cliente Android + Base de datos) para determinar el alcance exacto de la MVP y los gaps pendientes antes de declarar el producto completo de principio a fin.

---

## 1. Definición Funcional de la MVP

Para considerar **3D Cost Manager** como una MVP funcional de principio a fin, un usuario debe poder realizar el siguiente flujo ininterrumpido:
1. Registrarse e iniciar sesión (Autenticación JWT).
2. Crear un proyecto.
3. Configurar y asociar recursos de catálogo (máquinas, materiales, herramientas, mano de obra).
4. Definir parámetros económicos (márgenes de seguridad y beneficio).
5. Ejecutar el motor de cálculo de costes.
6. Generar y persistir un presupuesto (`Quote`).
7. Consultar los presupuestos generados posteriormente.

---

## 2. Auditoría por Áreas Funcionales

| Área | Backend / API | Base de datos / Persistencia | Cliente Android | Estado MVP |
| :--- | :--- | :--- | :--- | :--- |
| **Autenticacion** | Completo (`/api/v1/auth/*`) | Completo (`users`, `roles`) | Completo (Login, Register, TokenManager) | **COMPLETO** |
| **Proyectos** | Completo (`/api/v1/projects/*`) | Completo (`projects`) | Completo (List, Detail, Form) | **COMPLETO** |
| **Máquinas** | Completo (`/api/v1/machines/*`) | Completo (`machines`, `project_machines`) | Completo (List, Detail, Form) | **COMPLETO** |
| **Materiales** | Completo (`/api/v1/materials/*`) | Completo (`materials`, `project_materials`) | Completo (List, Detail, Form) | **COMPLETO** |
| **Herramientas (Tools)** | Completo (`/api/v1/tools/*` en backend) | Completo (`tools`, `project_tools`) | **Falta (Sin API ni UI en Android)** | **PARCIAL** |
| **Cálculo de Costes** | Completo (`CostCalculator` + `CostCalculationInput`) | N/A (Lógica de dominio pura) | **Falta (Sin UI de cálculo en Android)** | **PARCIAL** |
| **Presupuestos (Quotes)** | Completo (`/api/v1/projects/{id}/quotes`) | Completo (`quotes`) | **Falta (Sin API ni UI en Android)** | **PARCIAL** |

---

## 3. Matriz E2E del Flujo MVP

| Paso | Backend | Android | DB | Estado Actual |
| :--- | :--- | :--- | :--- | :--- |
| 1. Login / Registro | Operativo | Operativo | Operativo | **VERIFICADO** |
| 2. Crear Proyecto | Operativo | Operativo | Operativo | **VERIFICADO** |
| 3. Configurar Máquinas | Operativo | Operativo | Operativo | **VERIFICADO** |
| 4. Configurar Materiales | Operativo | Operativo | Operativo | **VERIFICADO** |
| 5. Configurar Herramientas | Operativo (API) | **Falta UI** | Operativo | **PARCIAL** |
| 6. Ejecutar Cálculo | Operativo (`CostCalculator`) | **Falta UI** | N/A | **PARCIAL** |
| 7. Generar Quote | Operativo (`QuoteService`) | **Falta UI** | Operativo | **PARCIAL** |
| 8. Consultar Quotes | Operativo (`QuoteController`) | **Falta UI** | Operativo | **PARCIAL** |

---

## 4. Clasificación de Funcionalidades (MVP / Post-MVP / Descartada)

### MVP (Necesario para declarar el producto funcional)
1. Cliente Android: Módulo y UI para **Herramientas (Tools)**.
2. Cliente Android: Módulo, Retrofit API y pantallas Compose para **Cálculo de Costes y Generación de Quotes**.
3. Cliente Android: Pantalla de consulta de **Presupuestos (Quotes)** asociados a un proyecto.

### Post-MVP (Interesante pero no bloqueante para el flujo principal)
1. Exportación de presupuestos a PDF.
2. Gráficos de desglose de costes en el cliente Android.
3. Gestión avanzada de estados comerciales de Quotes (`SENT`, `ACCEPTED`, `REJECTED`).

### Descartada (Fuera del alcance del producto)
1. Multi-idioma avanzado.
2. Notificaciones push en tiempo real.
3. Sincronización offline con base de datos local (Room).

---

## 5. Lista de Trabajo (Gaps a Implementar en Siguientes Issues)

Para cerrar la MVP, se han identificado las siguientes tareas concretas que constituirán las Issues posteriores:
* **Issue #30:** Implementación en Android de la gestión de **Herramientas (`ToolApi`, `ToolRepository`, `ToolViewModel`, pantallas Compose)**.
* **Issue #31:** Implementación en Android de la gestión de **Presupuestos (`QuoteApi`, `QuoteRepository`, `QuoteViewModel`, cálculo y pantallas de detalle/listado de Quotes)**.
* **Issue #32:** Pruebas E2E de integración cliente Android contra backend en entorno cloud/local y verificación del Quality Gate.
* **Issue #33:** Release final de la MVP (`Calculadora3D v1.0`).

---

## 6. Criterios de Aceptación de la Issue #29

- [x] Backend auditado de principio a fin.
- [x] Cliente Android auditado (identificando carencias en Tools y Quotes).
- [x] Matriz E2E completada.
- [x] Funcionalidades clasificadas en MVP, Post-MVP y Descartada.
- [x] Gaps documentados con precisión quirúrgica.
- [x] `docs/MVP_SCOPE_AUDIT.md` creado.
- [x] Definido el trabajo exacto previo a la declaración final de la MVP.
