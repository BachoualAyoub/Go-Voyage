plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val urlApi = providers.gradleProperty("GO_VOYAGE_API_URL").orElse("http://10.0.2.2:8081/").get()
require(urlApi.matches(Regex("https?://[^\\s\"]+/"))) { "GO_VOYAGE_API_URL doit se terminer par /." }

android {
    namespace = "tn.govoyage.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "tn.govoyage.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_URL", "\"$urlApi\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
}
