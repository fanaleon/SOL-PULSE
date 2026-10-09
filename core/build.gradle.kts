plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// Capa de datos compartida entre Pump Viewer (celu) y Pump Viewer TV:
// modelos, cliente de DexScreener, formato de números y repositorio persistente.
android {
    namespace = "app.pumpviewer.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
