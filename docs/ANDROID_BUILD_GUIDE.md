# Guía de compilación de la app Android — 3D Cost Manager

Cómo compilar la app, apuntarla al backend correcto e instalarla en un emulador o en un teléfono.

---

## 1. Requisitos

- **Android Studio** con el Android SDK 34 instalado.
- **JDK 21.** Sirve el que trae Android Studio (*Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*). Desde la terminal, Gradle usa `JAVA_HOME` o el `java` del `PATH`.
- **`android/local.properties`** con la ruta del SDK (`sdk.dir=...`). Android Studio lo crea al abrir el proyecto; no se versiona.

El repositorio no guarda rutas de ningún equipo. Si necesitas fijar el JDK para Gradle, añade `org.gradle.java.home` a tu `~/.gradle/gradle.properties`, no al del proyecto.

---

## 2. URL del backend (`BuildConfig.BASE_URL`)

La URL base se define en [`android/app/build.gradle.kts`](../android/app/build.gradle.kts) y la lee `NetworkConfig.kt`.

| Entorno | URL | Notas |
| :--- | :--- | :--- |
| Emulador (por defecto) | `http://10.0.2.2:8080/` | `10.0.2.2` es el `localhost` del equipo anfitrión visto desde el emulador. |
| Teléfono físico en la misma Wi-Fi | `http://<IP-del-PC>:8080/` | Cambia la URL del `buildType` que compiles. El firewall del PC debe aceptar conexiones entrantes al puerto 8080. |
| Producción | `https://...` | Aún no hay una URL HTTPS de producción. |

Hoy `debug` y `release` usan la misma URL de desarrollo y el manifiesto permite tráfico HTTP sin cifrar (`usesCleartextTraffic`). Separar la configuración por `buildType` es la Fase 6 del [roadmap](ROADMAP.md).

---

## 3. Compilar

Desde la carpeta `android`:

```bash
./gradlew test assembleDebug      # en Windows: gradlew.bat test assembleDebug
```

La APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`.

`./gradlew assembleRelease` genera `app-release-unsigned.apk`, pero **no está firmada ni apunta a un backend de producción**, así que no sirve para distribuirla.

El CI ([`android-ci.yml`](../.github/workflows/android-ci.yml)) ejecuta `test assembleDebug` en cada PR que toca `android/` y adjunta la APK debug como artefacto.

---

## 4. Instalar

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Que la APK compile no demuestra que la app funcione: con el backend en marcha, comprueba en el emulador o en el teléfono el registro, el inicio de sesión, el alta de catálogo, la creación de un proyecto y la generación de un presupuesto.

---

## 5. Sesión

El token JWT se guarda cifrado con `EncryptedSharedPreferences` (`TokenManager`). Si el backend responde 401 fuera de los endpoints de autenticación, la app borra el token y vuelve a la pantalla de inicio de sesión.
