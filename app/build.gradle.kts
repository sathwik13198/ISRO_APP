plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.isro_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.isro_app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {

    // MQTT (pure JVM client - no Android service, avoids LocalBroadcastManager crash)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // ===============================
    // 📍 LOCATION (GPS)
    // ===============================
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ===============================
    // CORE ANDROID + LIFECYCLE
    // ===============================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 🔥 REQUIRED for viewModel() inside Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation(libs.androidx.activity.compose)

    // ===============================
    // 🎨 COMPOSE
    // ===============================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended")

    // ===============================
    // 🗺️ OSMDROID (OFFLINE MAPS)
    // ===============================
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.osmdroid:osmdroid-wms:6.1.18")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ===============================
    // TESTING
    // ===============================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
