# Guía de Compilación y Ejecución del Cliente Android — 3D Cost Manager

Este documento detalla los pasos para compilar, generar el APK e instalar el cliente nativo Android de **3D Cost Manager** en un emulador o dispositivo físico.

---

## 1. Requisitos Previos

* **Android Studio Ladybug / Koala** (o superior) con Android SDK instalado (API level 34+).
* **JDK 17 o JDK 21** configurado en Android Studio.

---

## 2. Configuración de Red (Emulador vs. Dispositivo Físico)

* **Para Emulador Android (Por defecto):**
  El archivo `NetworkConfig.kt` está configurado con:
  ```kotlin
  private const val BASE_URL = "http://10.0.2.2:8080/"
  ```
  `10.0.2.2` es la dirección especial del emulador que apunta al `localhost` de tu máquina de desarrollo. Asegúrate de que el backend Spring Boot esté corriendo localmente en el puerto `8080`.

* **Para Dispositivo Físico:**
  Si vas a probar en un teléfono real conectado a la misma red Wi-Fi que tu ordenador, debes cambiar `BASE_URL` en `NetworkConfig.kt` por la dirección IP local de tu PC (ej. `http://192.168.1.50:8080/`).

---

## 3. Generación del APK (Android Studio o Terminal)

### Opción A — Desde Android Studio (Recomendado)
1. Abre la carpeta `android` como proyecto en Android Studio.
2. Espera a que sincronice Gradle.
3. Selecciona el menú superior **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Una vez finalice, Android Studio mostrará una notificación flotante con un enlace **locate** que abrirá la carpeta donde se encuentra el archivo `app-debug.apk`.

### Opción B — Desde la Terminal (Gradle Wrapper)
Ubicado en la carpeta `android`:
```bash
cd android
./gradlew assembleDebug
```
El APK generado se ubicará en:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Instalación en Emulador o Dispositivo

Puedes instalar el APK generado utilizando `adb` desde la terminal:

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

O arrastrando el archivo `app-debug.apk` directamente sobre la pantalla del emulador abierto en Android Studio.

---

## 5. Verificación del Flujo MVP en Android

Una vez instalada y abierta la aplicación:
1. **Login / Registro:** Inicia sesión con tus credenciales o regístrate en el sistema.
2. **Proyectos:** Visualiza el listado de proyectos y crea uno nuevo.
3. **Recursos de Catálogo:** Accede desde la barra superior a **Materiales**, **Máquinas** y **Herramientas (Tools)** para crear y gestionar recursos.
4. **Cálculo y Presupuestos (Quotes):** Entra en el detalle de un proyecto, pulsa en *"Calcular Costes y Generar Presupuesto"*, introduce los márgenes deseados y emite la `Quote`. Consulta el historial de presupuestos emitidos en *"Ver Presupuestos Emitidos"*.
