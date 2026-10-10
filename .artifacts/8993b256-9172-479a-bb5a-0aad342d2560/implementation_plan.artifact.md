# Plan de Implementación — Issue #35: Refinamiento UX/UI Base y Mensajes de Error Amigables en Android

Este plan detalla los pasos para implementar el refinamiento visual en Android (Material 3) y la centralización de mensajes de error amigables para el usuario (mapeando códigos HTTP como 409 a "Este usuario ya existe", 401 a "Correo o contraseña incorrectos", etc.).

## User Review Required

> [!IMPORTANT]
> **Traducción de Errores y UI:** `NetworkError.kt` centralizará la conversión de excepciones y códigos HTTP a mensajes limpios y comprensibles, eliminando códigos técnicos (como HTTP 409) de la interfaz de usuario en `LoginScreen`, `RegisterScreen` y `ProjectListScreen`.

## Open Questions

- Ninguna.

## Proposed Changes

### Android Client

#### [MODIFY] [NetworkError.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/domain/util/NetworkError.kt)
- Centralizar mapeo de códigos HTTP (400, 401, 403, 404, 409, 5xx) a mensajes amigables en español.

#### [MODIFY] [LoginScreen.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/auth/LoginScreen.kt)
#### [MODIFY] [RegisterScreen.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/auth/RegisterScreen.kt)
- Integrar mensajes amigables y pulir espaciado/jerarquía visual en Material 3.

#### [MODIFY] [ProjectListScreen.kt](file:///C:/Users/lovei/Documents/XD/Calculadora3D/android/app/src/main/java/com/tdcostmanager/app/ui/project/ProjectListScreen.kt)
- Refinar tarjetas de proyecto, tipografía y distribución visual.

## Verification Plan

### Automated Tests & Verification
1. Compilar proyecto Android (`./gradlew assembleDebug` o desde Android Studio).
2. Verificación en emulador:
   - Registro con correo existente → mostrar "Este usuario ya existe".
   - Login con credenciales incorrectas → mostrar "Correo o contraseña incorrectos".
   - Comprobación visual de pantallas refinadas.
