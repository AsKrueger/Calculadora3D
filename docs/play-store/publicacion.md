# Publicar 3D Cost Manager en Google Play

Respuestas listas para copiar en Play Console. El orden es el de la pantalla «Panel» de la app nueva.

## 1. Crear la app

| Campo | Valor |
| :--- | :--- |
| Nombre | 3D Cost Manager: costes 3D |
| Idioma predeterminado | Español (España) – es-ES |
| App o juego | App |
| Gratis o de pago | Gratis |
| ID del paquete | `com.tdcostmanager.app` (lo fija el primer `.aab` que subas; no se puede cambiar) |

## 2. Configurar la app

### Política de privacidad
`https://askrueger.github.io/3d-cost-manager/privacy.html`

### Acceso a la app
Toda la funcionalidad está disponible sin restricciones (no hay inicio de sesión).

### Anuncios
Sí, la app contiene anuncios.

### Clasificación de contenido (cuestionario IARC)
- Categoría: **Todas las demás tipos de apps** (utilidad / productividad).
- Violencia, sexo, lenguaje, drogas, juego con dinero real: **No** a todo.
- ¿Los usuarios pueden interactuar o compartir contenido entre ellos? **No** (los PDF se comparten con apps externas, no dentro de la app).
- ¿Comparte la ubicación del usuario con otros? **No**.
- ¿Permite compras digitales? **Sí** («Quitar anuncios»).
- Resultado esperado: PEGI 3 / Everyone.

### Público objetivo
- Grupos de edad: **18 años o más**. (La app no está pensada para menores y así no se aplican los requisitos de la política de Familias.)
- ¿Atrae a niños sin querer? **No**.

### Aplicaciones de noticias
No es una app de noticias.

### Seguridad de los datos
Según la [guía de Google para el SDK de anuncios](https://developers.google.com/admob/android/privacy/play-data-disclosure):

- ¿La app recoge o comparte datos de usuario? **Sí** (lo hace el SDK de Google Mobile Ads).
- ¿Se cifran en tránsito? **Sí**.
- ¿Pueden los usuarios pedir que se eliminen? **No** (la app no guarda datos en servidores; los datos de anuncios los gestiona Google).

| Tipo de datos | Recogido | Compartido | Uso | Opcional |
| :--- | :--- | :--- | :--- | :--- |
| Ubicación › Aproximada (IP) | Sí | Sí | Publicidad, Analíticas, Prevención de fraude | No |
| Actividad en apps › Interacciones con la app | Sí | Sí | Publicidad, Analíticas, Prevención de fraude | No |
| Información y rendimiento de la app › Registros de fallos y diagnósticos | Sí | Sí | Analíticas, Prevención de fraude | No |
| Dispositivo u otros identificadores › ID de publicidad | Sí | Sí | Publicidad, Analíticas, Prevención de fraude | No |

No se declara nada más: piezas, presupuestos, clientes y fotos se quedan en el móvil y nunca salen de él. Los comentarios se envían con la app de correo del usuario.

### Categoría y contacto
- Categoría: **Herramientas**.
- Etiquetas: Calculadora, Productividad, Negocios.
- Correo de contacto: el de soporte que quieras mostrar.
- Sitio web: `https://askrueger.github.io` (necesario para que AdMob valide `app-ads.txt`).

## 3. Ficha de Play Store

- Textos: [`ficha-es.md`](ficha-es.md) y [`listing-en.md`](listing-en.md) (añade «Inglés (Estados Unidos)» como traducción).
- Icono 512 × 512 y gráfico de funciones 1024 × 500: en [`graficos/`](graficos/).
- Capturas de teléfono (mínimo 2, recomendado 4–8): en [`graficos/`](graficos/).

## 4. Producto «Quitar anuncios»

Monetizar › Productos › Productos integrados › Crear producto:
- ID: `remove_ads` (exactamente así)
- Nombre: «Quitar anuncios»
- Descripción: «Quita la barra de anuncios para siempre. Pago único.»
- Precio: el que decidas (2,99 € es habitual en este tipo de apps).
- Activar.

## 5. Subir la versión

1. Pruebas › Prueba cerrada › Crear versión.
2. Firma de apps de Google Play: aceptar (Google guarda la clave final; tú subes con la clave de subida).
3. Subir `android/app/build/outputs/bundle/release/app-release.aab` (se genera con `./gradlew bundleRelease`).
4. Notas de la versión: las de la ficha.
5. Testers: tu lista de correos (las cuentas personales nuevas necesitan 12 testers durante 14 días antes de producción).

## 6. Clave de subida

- Archivo: `Documents/XD/claves-android/3d-cost-manager-upload.jks` (fuera del repositorio).
- Contraseñas: en `android/keystore.properties` (no se versiona) y su copia `claves-android/keystore.properties.copia`.
- **Haz una copia de la carpeta `claves-android` en un lugar seguro** (USB, nube privada). Si se pierde, hay que pedir a Google que restablezca la clave de subida.

## 7. AdMob

1. Crear la app en AdMob (Android, «no publicada») y un bloque **Banner**.
2. Poner los ID en `~/.gradle/gradle.properties`:
   ```properties
   ADMOB_APP_ID=ca-app-pub-5629298070616122~XXXXXXXXXX
   ADMOB_BANNER_ID=ca-app-pub-5629298070616122/YYYYYYYYYY
   ```
3. Volver a generar el `.aab` y subirlo.
4. Cuando la app esté en Google Play, vincularla en AdMob (Configuración de la app › Tienda).
