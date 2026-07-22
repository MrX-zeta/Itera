plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.luis.itera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luis.itera"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "2.0"
    }

    signingConfigs {
        // Si faltan las credenciales, NO se crea el signingConfig "release" (antes se creaba
        // igual y el buildType caía a signingConfig = null, produciendo un APK SIN FIRMAR en
        // silencio). El fallo explícito vive más abajo, en el chequeo de taskGraph: así el
        // mensaje de error es claro en vez de un fallo críptico de AGP por falta de config.
        val storePwd = System.getenv("ITERA_STORE_PASSWORD")
        val keyPwd = System.getenv("ITERA_KEY_PASSWORD")
        if (!storePwd.isNullOrBlank() && !keyPwd.isNullOrBlank()) {
            create("release") {
                storeFile = file("${System.getProperty("user.home")}/itera-release-new.jks")
                storePassword = storePwd
                keyAlias = "itera"
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = try { signingConfigs.getByName("release") } catch (_: Exception) { null }
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
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Falla RUIDOSAMENTE si se intenta empaquetar/instalar release sin las credenciales de firma,
// en vez de dejar que AGP siga con signingConfig = null y produzca un APK sin firmar (o falle
// más tarde con un error críptico de "keystore not set"). Solo mira tareas de RELEASE
// (assemble/bundle/install): el build de debug corre siempre igual, sin tocar nada de esto.
gradle.taskGraph.whenReady {
    val releaseTaskRequested = allTasks.any { task ->
        task.path.contains(":app:") &&
            Regex("(assemble|bundle|install).*Release").containsMatchIn(task.name)
    }
    if (releaseTaskRequested) {
        val storePwd = System.getenv("ITERA_STORE_PASSWORD")
        val keyPwd = System.getenv("ITERA_KEY_PASSWORD")
        if (storePwd.isNullOrBlank() || keyPwd.isNullOrBlank()) {
            throw GradleException(
                "Faltan las variables de entorno de firma: ITERA_STORE_PASSWORD y/o " +
                    "ITERA_KEY_PASSWORD. El build de release no puede continuar sin ellas " +
                    "(no se genera un APK/AAB sin firmar)."
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    testImplementation("junit:junit:4.13.2")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
}