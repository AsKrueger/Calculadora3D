# Roadmap de 3D Cost Manager

Plan de mejoras surgido de la revisión del código (octubre 2026). Las fases van por prioridad: primero seguridad, después integridad de los datos, luego funcionalidad y por último rendimiento y despliegue. Cada fase se entrega en su propia rama y PR contra `develop`.

Estado: ⬜ pendiente · 🟡 en curso · ✅ hecho

---

## Fase 1 — Endurecer la configuración de seguridad ✅

Cambios pequeños que cierran agujeros de configuración sin tocar el dominio.

- [x] Quitar el valor por defecto de `jwt.secret` en `application.properties`; si no se define `JWT_SECRET`, la aplicación no arranca.
- [x] Validar al arrancar que el secreto es Base64 válido y tiene al menos 256 bits, con un mensaje de error claro.
- [x] Corregir `.env.example` con un valor Base64 válido y documentar cómo generarlo (`openssl rand -base64 32`).
- [x] Restringir `/actuator/**`: solo `/actuator/health` público, el resto autenticado.
- [x] Desactivar `spring.jpa.show-sql` por defecto.
- [x] Sacar `.artifacts/` y las carpetas de `.idea/` del repositorio y añadirlas a `.gitignore`.
- [x] Tests unitarios de la validación del secreto.
- [x] Arreglar el CI: `backend/mvnw` no tenía permiso de ejecución y el job fallaba siempre (exit 126).

## Fase 2 — Separar los datos por usuario ✅

El fallo más grave: cualquier usuario registrado veía y modificaba los datos de todos. Issue #42.

- [x] Migración `V6__Add_Owner_To_Domain.sql`: columna `owner_id` (FK a `users`, `NOT NULL`) con índice en materiales, máquinas, herramientas y proyectos. Los presupuestos heredan el propietario a través de su proyecto.
- [x] Datos existentes: se asignan al usuario más antiguo. Si hay datos pero ningún usuario, se crea `legacy-owner@localhost` deshabilitado (no puede iniciar sesión) para no perder información.
- [x] `CurrentUserProvider` obtiene el usuario autenticado del `SecurityContext`; los servicios asignan el propietario al crear y filtran todas las lecturas, ediciones y borrados por `owner_id`. Nunca se acepta un propietario enviado por el cliente.
- [x] Devolver 404 (no 403) al acceder a recursos de otro usuario, para no revelar que existen.
- [x] Impedir que un proyecto use materiales, máquinas o herramientas de otro usuario (404).
- [x] `GET /projects/{projectId}/quotes/{quoteId}` comprueba que el presupuesto pertenece a ese proyecto y al usuario.
- [x] Tests de integración con dos usuarios y tokens JWT reales (`OwnershipIsolationIntegrationTest`).

## Fase 3 — Presupuestos que se puedan auditar ✅

Que un presupuesto guarde cómo se calculó. Issue #44.

- [x] Migración `V7__Add_Breakdown_To_Quotes.sql`: desglose por partida (material, máquina, herramienta, mano de obra, electricidad), seguridad, beneficio, energía consumida, precio del kWh y `calculated_at` (`TIMESTAMPTZ`). En los presupuestos existentes, la seguridad y el beneficio se deducen de los importes guardados y `calculated_at` toma `created_at`; el resto del desglose queda `NULL`.
- [x] `CostCalculator` devuelve también la energía y el precio del kWh, y no consulta ESIOS si el consumo es 0. `QuoteService` guarda el desglose completo e interpreta la fecha del cálculo en `Europe/Madrid`.
- [x] Ciclo de vida en `QuoteStatus`: `DRAFT → SENT | REJECTED`, `SENT → ACCEPTED | REJECTED | EXPIRED`; el resto, 409.
- [x] `PATCH /api/v1/projects/{projectId}/quotes/{quoteId}/status` para cambiar el estado (404 si el presupuesto es de otro usuario).
- [x] La app Android muestra el desglose, permite cambiar el estado y enseña el mensaje del backend en los errores 400 y 409.

## Fase 4 — Rediseño de la app Android ✅

La app funcionaba, pero no se podía enseñar: sin tema propio, formatos de programador y pantallas que prometían funciones que no existían. Issue #48.

- [x] Tema propio «taller técnico» (grafito y naranja filamento) en modo claro y oscuro, con componentes compartidos (`ui/theme`, `ui/common`).
- [x] Formato español en importes, cantidades y fechas; estados y categorías traducidos.
- [x] Barra inferior (Proyectos, Catálogo con pestañas, Cuenta), cierre de sesión y arreglo de la pantalla en blanco al volver atrás tras iniciar sesión.
- [x] Detalle de proyecto con sus líneas; presupuesto con barra de desglose por partida; coste unitario en el catálogo.
- [x] Formularios que aceptan coma decimal, con validación por campo y todas las categorías y unidades.
- [x] Login y registro sin funciones ficticias; tras registrarse se inicia sesión sola.
- [x] Icono adaptativo y splash propios.
- [x] `.gitattributes` para que `mvnw` y `gradlew` conserven LF en Windows (`docker compose up --build` fallaba).
- [x] Login y registro según el diseño de referencia: logo con degradado, campos con etiqueta y botón en degradado, reutilizables (Issue #49).

## Fase 5 — Proyectos editables y validaciones coherentes ⬜

Issue #45.

- [ ] Permitir añadir, modificar y quitar líneas (materiales, máquinas, herramientas) en `ProjectUpdateRequest` y `Project`.
- [ ] Exigir cantidad mayor que 0 en la API y en la base de datos (`CHECK quantity > 0`), para que falle al guardar y no al presupuestar.
- [ ] Conversión entre unidades de masa y volumen mediante la densidad del material (p. ej. resina comprada en litros y usada en gramos).

## Fase 6 — Rendimiento y robustez ⬜

Issue #46.

- [ ] Eliminar las consultas N+1 en `ProjectService.findAll` (`@EntityGraph` o `JOIN FETCH`).
- [ ] Caché de precios de ESIOS por hora.
- [ ] Excepción propia para los fallos de ESIOS, traducida a 503 (hoy un token ausente responde 409 y un precio no encontrado, 500).
- [ ] Usar la zona `Europe/Madrid` al consultar ESIOS, para acertar el tramo en los días de cambio de hora.
- [ ] Migración: `users` con `TIMESTAMPTZ` y eliminar el índice redundante `idx_users_email`.

## Fase 7 — App Android lista para release ⬜

Issue #47.

- [ ] `BASE_URL` distinta por `buildType` (emulador en debug, HTTPS en release).
- [ ] `usesCleartextTraffic` solo en debug (network security config).
- [ ] `isMinifyEnabled = true` con reglas de R8/ProGuard para Retrofit y modelos.
- [ ] `allowBackup="false"` o reglas de backup que excluyan el token.

## Fase 8 — Recuperación de contraseña ⬜

Issue #50. Hoy, quien olvida la contraseña pierde el acceso a sus datos.

- [ ] Migración `password_reset_tokens`: código de 6 dígitos con hash, caducidad de 15 minutos y un solo uso.
- [ ] `POST /auth/password-reset/request` (responde siempre 202, sin revelar si el correo existe) y `POST /auth/password-reset/confirm`.
- [ ] Límite de intentos por código y de solicitudes por correo.
- [ ] Envío por SMTP (`spring-boot-starter-mail`) configurado con variables de entorno y Parameter Store.
- [ ] Enlace «¿Olvidaste?» en el login y pantallas para pedir el código y fijar la nueva contraseña.

---

## Fuera de este roadmap (ideas para más adelante)

- Refresh tokens.
- Exportar presupuestos a PDF.
- Límite de intentos en el login.
