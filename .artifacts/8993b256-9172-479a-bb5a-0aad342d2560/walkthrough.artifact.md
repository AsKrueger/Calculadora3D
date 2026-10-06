# Walkthrough — Issue #31: Android — Cálculo de Costes y Gestión de Quotes

Se ha completado exitosamente la integración completa del flujo de **Cálculo de Costes y Presupuestos (Quotes)** en el cliente Android, respetando que el backend es la fuente de verdad del cálculo económico.

## Entregables y Acciones Realizadas

### 1. Cliente Android (`Quotes`)
- **DTOs (`QuoteDto.kt` & `QuoteStatus.kt`):** Modelos serializables (`QuoteResponse`, `QuoteCreateRequest`, `QuoteStatus`) alineados exactamente con los contratos REST del backend.
- **API Retrofit (`QuoteApi.kt`):** Interfaz para los endpoints `/api/v1/projects/{projectId}/quotes` (creación de presupuestos mediante cálculo, listado y consulta de detalle).
- **Repositorio (`QuoteRepository.kt`):** Abstracción de red que gestiona llamadas suspendidas y devuelve objetos `Result<T>`.
- **ViewModel (`QuoteViewModel.kt`):** Gestiona reactivamente el estado de cálculo y consulta de Quotes mediante `StateFlow` y `UiState`.
- **UI Jetpack Compose:**
  - `QuoteCalculatorScreen.kt`: Pantalla para configurar márgenes (beneficio y seguridad) y solicitar el cálculo y emisión del presupuesto al backend.
  - `QuoteListScreen.kt`: Listado de presupuestos emitidos para un proyecto.
  - `QuoteDetailScreen.kt`: Consulta detallada de un presupuesto (coste base, ajustado, precio final y márgenes).
- **Integración y Navegación:** Actualización de `NetworkConfig`, `ViewModelFactory`, `NavGraph` y `ProjectDetailScreen` para permitir acceder al cálculo de costes y listado de Quotes directamente desde el detalle de cada proyecto.

## Resultados de Verificación
- **Backend Build & Tests:** **`BUILD SUCCESS`** (33 tests unitarios y puros de dominio pasados sin errores).
- **Flujo E2E MVP Cerrado:** El cliente Android ahora cubre todo el ciclo de negocio principal (Auth → Proyectos → Recursos → Cálculo → Emisión de Quote → Consulta).
