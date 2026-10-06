# Plan de Implementación — Issue #30: Gestión de Tools en Backend y Android

Este plan detalla los pasos para completar la gestión de **Tools (Herramientas)** tanto en el backend (creando DTOs, Service y Controller faltantes) como en el cliente Android (`ToolApi`, `ToolRepository`, `ToolViewModel` y pantallas Jetpack Compose).

## User Review Required

> [!IMPORTANT]
> **Completitud del Backend:** Aunque la entidad `Tool` y el repositorio existían, faltaban los DTOs, el `ToolService` y el `ToolController`. Esta issue implementará la API REST completa para Tools antes de conectar el cliente Android.

## Open Questions

- Ninguna.

## Proposed Changes

### Backend

#### [NEW] [ToolCreateRequest.java](file:///C:/Users/lovei/Documents/XD/Calculadora3D/backend/src/main/java/com/tdcostmanager/backend/application/dto/ToolCreateRequest.java)
#### [NEW] [ToolUpdateRequest.java](file:///C:/Users/lovei/Documents/XD/Calculadora3D/backend/src/main/java/com/tdcostmanager/backend/application/dto/ToolUpdateRequest.java)
#### [NEW] [ToolResponse.java](file:///C:/Users/lovei/Documents/XD/Calculadora3D/backend/src/main/java/com/tdcostmanager/backend/application/dto/ToolResponse.java)
- DTOs de validación y transferencia para Tools.

#### [NEW] [ToolService.java](file:///C:/Users/lovei/Documents/XD/Calculadora3D/backend/src/main/java/com/tdcostmanager/backend/application/service/ToolService.java)
- Lógica transaccional para CRUD y desactivación de herramientas.

#### [NEW] [ToolController.java](file:///C:/Users/lovei/Documents/XD/Calculadora3D/backend/src/main/java/com/tdcostmanager/backend/application/controller/ToolController.java)
- Endpoints REST en `/api/v1/tools`.

### Android Client

#### [NEW] [ToolDto.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/remote/dto/tool/ToolDto.kt)
- DTOs serializables con Kotlinx Serialization para Tools.

#### [NEW] [ToolApi.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/remote/api/ToolApi.kt)
- Interfaz Retrofit para llamadas a `/api/v1/tools`.

#### [NEW] [ToolRepository.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/data/repository/ToolRepository.kt)
- Repositorio Android para abstracción de red de Tools.

#### [NEW] [ToolViewModel.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/tool/ToolViewModel.kt)
- ViewModel reactivo para estado de herramientas.

#### [NEW] UI Screens (`ToolListScreen.kt`, `ToolDetailScreen.kt`, `ToolFormScreen.kt`)
- Pantallas Jetpack Compose para listado, detalle y formulario de herramientas.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/navigation/NavGraph.kt)
#### [MODIFY] [ViewModelFactory.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/ViewModelFactory.kt)
- Integración de navegación y factoría de ViewModels para Tools.

## Verification Plan

### Automated Tests
1. Ejecutar tests unitarios y de integración en backend (`./mvnw.cmd clean verify`).
2. Verificar compilación correcta del proyecto Android.
