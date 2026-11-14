plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.max.misfinanzas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.max.misfinanzas"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1) + 100
        versionName = "2.0"
    }

    // Firma fija: así cada versión nueva se instala encima de la anterior sin borrar tus datos
    signingConfigs {
        create("misApps") {
            storeFile = rootProject.file("../firma/mis-apps.jks")
            storePassword = System.getenv("FIRMA_CLAVE") ?: "mis-apps-2026"
            keyAlias = "mis-apps"
            keyPassword = System.getenv("FIRMA_CLAVE") ?: "mis-apps-2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("misApps")
        }
        debug {
            signingConfig = signingConfigs.getByName("misApps")
        }
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
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
