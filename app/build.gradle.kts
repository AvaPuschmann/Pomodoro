import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Read SUPABASE_URL and SUPABASE_ANON_KEY from local.properties (not committed to Git)
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())

// Fail-fast guard : refuse de build si les credentials Supabase manquent.
// Hot-fix 2026-05-23 — bug critique Philippe : local.properties avait perdu
// SUPABASE_URL et SUPABASE_ANON_KEY (perdu silencieusement lors d'un reset
// IDE), build a passé avec valeurs vides → supabase-kt a fallback sur
// https://localhost → ECONNREFUSED → mapping français "Impossible de se
// connecter. Vérifiez votre connexion." trompeur. Cf. local.properties.example.
// Skip pour les tâches Gradle qui n'ont pas besoin du build (sync, clean, etc.).
val isAssembleTask = gradle.startParameter.taskNames.any { name ->
    name.contains("assemble", ignoreCase = true) ||
    name.contains("install", ignoreCase = true) ||
    name.contains("bundle", ignoreCase = true)
}
if (isAssembleTask) {
    val supabaseUrl = localProps["SUPABASE_URL"]?.toString().orEmpty()
    val supabaseKey = localProps["SUPABASE_ANON_KEY"]?.toString().orEmpty()
    if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
        throw GradleException(
            """

            ❌ Build refused: missing Supabase credentials in local.properties.

            Expected variables (see local.properties.example) :
                SUPABASE_URL=https://<project>.supabase.co
                SUPABASE_ANON_KEY=eyJ...

            Current values :
                SUPABASE_URL     = "${supabaseUrl.ifBlank { "<empty>" }}"
                SUPABASE_ANON_KEY = "${if (supabaseKey.isBlank()) "<empty>" else supabaseKey.take(12) + "..."}"

            Without these, supabase-kt falls back to https://localhost and
            all auth/sync calls fail with ECONNREFUSED + the misleading
            "Impossible de se connecter. Vérifiez votre connexion." message
            on the login screen.

            Fix : edit local.properties (NOT local.properties.example) and
            paste the values from Supabase Studio → Project Settings → API.
            """.trimIndent()
        )
    }
}

android {
    namespace = "com.agenticfocus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agenticfocus"
        minSdk = 31
        targetSdk = 35
        // Story 31-7 — aligné sur le desktop (1.4.0/1.4.1). La version mobile était figée à
        // 1.1.0 depuis mars : l'écran « À propos » affichait la même chose quel que soit le
        // code installé, et ne permettait donc pas de vérifier qu'un Run ▶ avait bien pris.
        versionCode = 3
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProps["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps["SUPABASE_ANON_KEY"] ?: ""}\"")

        // Mode Projet feature flags (Story 16-1) — defaults true, override via local.properties
        buildConfigField("Boolean", "FEATURE_PROJECTS", localProps["FEATURE_PROJECTS"]?.toString() ?: "true")
        buildConfigField("Boolean", "FEATURE_KANBAN_UI", localProps["FEATURE_KANBAN_UI"]?.toString() ?: "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Story 31-3 — sans ceci, tout appel à android.util.Log depuis du code de production
    // atteint en test unitaire lève « Method d in android.util.Log not mocked » et fait
    // échouer le test pour une raison sans rapport avec ce qu'il vérifie. Le manque était
    // resté invisible tant que les sources de test ne compilaient plus.
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.reorderable)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.postgrest)
    // OkHttp engine — supports WebSocket (required for Supabase Realtime).
    // ktor-client-android does NOT support WebSocketCapability, causing
    // "Engine doesn't support WebSocketCapability" error every 7s in logcat.
    runtimeOnly(libs.ktor.client.okhttp)

    implementation(libs.androidx.core.splashscreen)

    // Security — EncryptedSharedPreferences
    implementation(libs.security.crypto)

    // WorkManager — Story 24-6 Sprint 22 Epic 24 (notification quotidienne Bilan du Jour)
    implementation(libs.androidx.work.runtime.ktx)

    // Markwon — rendu Markdown (note de tâche, éditeur Agrandir). Lib Android stable (Maven Central).
    implementation(libs.markwon.core)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
