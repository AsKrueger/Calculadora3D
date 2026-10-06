# Plan de Implementación (Revisado) — Issue #31: Android — Cálculo de Costes y Gestión de Quotes

Este plan detalla los pasos para completar la integración del motor de cálculo y la gestión de presupuestos (`Quotes`) en el cliente Android, respetando estrictamente que el backend es la fuente de verdad del cálculo económico.

## User Review Required

> [!IMPORTANT]
> **Cálculo Backend-Driven:** Android no duplicará ninguna fórmula financiera. El flujo enviará los parámetros económicos (`marginPercentage`, `safetyPercentage`, `calculationDateTime`) al endpoint `POST /api/v1/projects/{projectId}/quotes`, donde el backend invocará `CostCalculator` y persistirá la `Quote` resultante.

## Open Questions

- Ninguna.

## Proposed Changes

### Android Client

#### [NEW] [QuoteDto.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/remote/dto/quote/QuoteDto.kt)
- DTOs serializables adaptados exactamente a los contratos del backend (`QuoteResponse`, `QuoteCreateRequest`, `QuoteStatus`).

#### [NEW] [QuoteApi.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/remote/api/QuoteApi.kt)
- Interfaz Retrofit para endpoints de Quotes bajo `/api/v1/projects/{projectId}/quotes`.

#### [NEW] [QuoteRepository.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/repository/QuoteRepository.kt)
- Repositorio Android para abstracción de red de Quotes.

#### [NEW] [QuoteViewModel.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/quote/QuoteViewModel.kt)
- ViewModel reactivo para estado de cálculo y listado/detalle de Quotes (sin lógica financiera duplicada).

#### [NEW] UI Screens (`QuoteCalculatorScreen.kt`, `QuoteListScreen.kt`, `QuoteDetailScreen.kt`)
- Pantallas Jetpack Compose para configurar márgenes, calcular/generar presupuesto, listar y consultar presupuestos.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/navigation/NavGraph.kt)
#### [MODIFY] [ViewModelFactory.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/ViewModelFactory.kt)
#### [MODIFY] [ProjectDetailScreen.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/project/ProjectDetailScreen.kt)
- Integración de navegación, factoría de ViewModels y botones de acceso en el detalle de proyectos.

## Verification Plan

### Automated Tests & Verification
1. Verificación de DTOs contra contratos backend.
2. Compilación correcta del proyecto Android.
3. Verificación funcional de:
   - Creación de Quote (backend-driven).
   - Listado de Quotes por proyecto.
   - Detalle de Quote.
   - Estados de carga, éxito y error en UI.
   - Autenticación JWT y navegación integrada.
