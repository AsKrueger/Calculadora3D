# Guía de Compilación y Configuración de Entornos Android — 3D Cost Manager

Este documento detalla los pasos para compilar, configurar endpoints por entorno e instalar el cliente nativo Android de **3D Cost Manager** en un emulador o dispositivo físico.

---

## 1. Requisitos Previos

* **Android Studio 2024.1+** con Android SDK instalado (API level 34+).
* **JDK 21** configurado en Android Studio (`jbr-21`).

---

## 2. Configuración Centralizada de URL Base (`BuildConfig.BASE_URL`)

Desde la **Issue #38**, la URL base de la API REST se encuentra centralizada a través de `BuildConfig.BASE_URL` definido en `android/app/build.gradle.kts`. `NetworkConfig.kt` consume directamente esta propiedad.

### Entornos de Red

1. **Emulador Android (Desarrollo por Defecto):**
   - Configuración: `buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")`
   - La IP `10.0.2.2` permite al emulador redirigir el tráfico hacia el `localhost` del host de desarrollo en el puerto `8080`.

2. **Dispositivo Físico (Pruebas en LAN local):**
   - Para probar en un terminal físico en la misma red Wi-Fi, edita temporalmente `build.gradle.kts` o define una propiedad en `gradle.properties`:
     ```kotlin
     buildConfigField("String", "BASE_URL", "\"http://192.168.X.X:8080/\"")
     ```
   - *Nota:* Asegúrate de que el firewall de tu PC permita conexiones entrantes al puerto 8080.

3. **Staging / Producción (Nube HTTPS):**
   - Cuando el backend se encuentre desplegado en la nube AWS con TLS/HTTPS habilitado, configura en la variante `release`:
     ```kotlin
     buildConfigField("String", "BASE_URL", "\"https://api.3dcostmanager.com/\"")
     ```

---

## 3. Generación de APKs (Android Studio o Terminal)

### Opción A — Desde la Terminal (Gradle Wrapper)
Ubicado en la carpeta `android`:
```bash
cd android
./gradlew assembleDebug assembleRelease
```
Ubicación de artefactos generados:
- **Debug:** `android/app/build/outputs/apk/debug/app-debug.apk`
- **Release:** `android/app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 4. Instalación en Emulador o Dispositivo

Puedes instalar el APK generado utilizando `adb` desde la terminal:

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Verificación de Integridad y Calidad

- **Compilación sin advertencias:** Los formularios Compose (`MachineFormScreen`, `MaterialFormScreen`, `ProjectFormScreen`, `ToolFormScreen`) fueron saneados de expresiones redundantes (`Condition is always 'true'`).
- **Seguridad local:** Las credenciales y tokens JWT se almacenan en `EncryptedSharedPreferences` protegidos por la Keystore del dispositivo.
