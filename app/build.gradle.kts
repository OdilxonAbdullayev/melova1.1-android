plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun escaped(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "uz.melova.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "uz.melova.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"
        val url = providers.environmentVariable("MELOVA_SUPABASE_URL").orElse("").get().trim().trimEnd('/')
        val key = providers.environmentVariable("MELOVA_SUPABASE_PUBLISHABLE_KEY").orElse("").get().trim()
        buildConfigField("String", "SUPABASE_URL", "\"${escaped(url)}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${escaped(key)}\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
