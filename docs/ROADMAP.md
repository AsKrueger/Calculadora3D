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

## Fase 3 — Presupuestos que se puedan auditar ⬜

Que un presupuesto guarde cómo se calculó, como promete el README.

- [ ] Migración: desglose por partida (material, máquina, herramienta, mano de obra, electricidad), precio del kWh usado y fecha/hora del cálculo (`TIMESTAMPTZ`).
- [ ] Rellenar esos campos desde `CostCalculator` al crear el presupuesto.
- [ ] Máquina de estados en `Quote.updateStatus` con transiciones permitidas; 409 si la transición no es válida.
- [ ] Exponer el desglose en la API y en la app Android.

## Fase 4 — Proyectos editables y validaciones coherentes ⬜

- [ ] Permitir añadir, modificar y quitar líneas (materiales, máquinas, herramientas) en `ProjectUpdateRequest` y `Project`.
- [ ] Exigir cantidad mayor que 0 en la API y en la base de datos (`CHECK quantity > 0`), para que falle al guardar y no al presupuestar.
- [ ] Conversión entre unidades de masa y volumen mediante la densidad del material (p. ej. resina comprada en litros y usada en gramos).

## Fase 5 — Rendimiento y robustez ⬜

- [ ] Eliminar las consultas N+1 en `ProjectService.findAll` (`@EntityGraph` o `JOIN FETCH`).
- [ ] Caché de precios de ESIOS por hora.
- [ ] Usar la zona `Europe/Madrid` al consultar ESIOS, para acertar el tramo en los días de cambio de hora.
- [ ] Migración: `users` con `TIMESTAMPTZ` y eliminar el índice redundante `idx_users_email`.

## Fase 6 — App Android lista para release ⬜

- [ ] `BASE_URL` distinta por `buildType` (emulador en debug, HTTPS en release).
- [ ] `usesCleartextTraffic` solo en debug (network security config).
- [ ] `isMinifyEnabled = true` con reglas de R8/ProGuard para Retrofit y modelos.
- [ ] `allowBackup="false"` o reglas de backup que excluyan el token.

---

## Fuera de este roadmap (ideas para más adelante)

- Refresh tokens y cierre de sesión.
- Exportar presupuestos a PDF.
- Límite de intentos en el login.
