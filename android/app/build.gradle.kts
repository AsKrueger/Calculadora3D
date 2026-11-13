import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Firma de release: solo si existe android/keystore.properties (no se versiona).
// Claves: storeFile, storePassword, keyAlias, keyPassword.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// AdMob: los identificadores reales se ponen en ~/.gradle/gradle.properties (ADMOB_APP_ID y ADMOB_BANNER_ID).
// La release solo lleva anuncios si existen: sin ellos sale sin barra, sin consentimiento, sin «Quitar anuncios»
// y sin el permiso de ID de publicidad. La versión de depuración siempre usa los anuncios de prueba de Google.
val admobAppId = findProperty("ADMOB_APP_ID") as String?
val admobBannerId = findProperty("ADMOB_BANNER_ID") as String?
val releaseAds = !admobAppId.isNullOrBlank() && !admobBannerId.isNullOrBlank()
if (admobAppId.isNullOrBlank() != admobBannerId.isNullOrBlank()) {
    throw GradleException("AdMob a medias: define ADMOB_APP_ID y ADMOB_BANNER_ID, o ninguno de los dos para publicar sin anuncios.")
}
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testAppId = "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "com.tdcostmanager.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tdcostmanager.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "FEEDBACK_EMAIL", "\"alonsoamadorsanchez@gmail.com\"")
    }

    androidResources {
        // Idiomas de la app: genera locales_config para el selector de idioma por app de Android 13+
        generateLocaleConfig = true
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Sin anuncios el SDK no se inicia; el ID de prueba solo evita que falle al instalar
            manifestPlaceholders["admobAppId"] = if (releaseAds) admobAppId!! else testAppId
            buildConfigField("boolean", "ADS_ENABLED", releaseAds.toString())
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${if (releaseAds) admobBannerId else ""}\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    if (!releaseAds) {
        sourceSets.getByName("release").manifest.srcFile("src/noads/AndroidManifest.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Datos locales
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Publicidad y consentimiento (UE)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // Pago único «Quitar anuncios» y valoración dentro de la app
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.play:review-ktx:2.0.2")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
