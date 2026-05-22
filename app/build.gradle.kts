import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.idsr_project"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.idsr_project"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SERVER_IP", "\"${localProperties.getProperty("SERVER_IP", "192.168.1.3")}\"")
        buildConfigField("String", "SERVER_PORT", "\"${localProperties.getProperty("SERVER_PORT", "5000")}\"")
        buildConfigField("String", "PINNED_HOST",     "\"${localProperties.getProperty("PINNED_HOST",     "")}\"")
        buildConfigField("String", "SSL_PIN_PRIMARY", "\"${localProperties.getProperty("SSL_PIN_PRIMARY", "")}\"")
        buildConfigField("String", "SSL_PIN_BACKUP",  "\"${localProperties.getProperty("SSL_PIN_BACKUP",  "")}\"")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Retrofit + Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Maps
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Charts
    implementation(libs.mpandroidchart)

    // Images
    implementation(libs.glide)
    implementation(libs.photoview)

    // UI
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.konfetti.xml)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Coroutines
    implementation(libs.coroutines.android)

    // Firebase
    implementation(libs.com.google.firebase.firebase.messaging.ktx2)
    implementation(libs.firebase.crashlytics.ktx.v1941)
    implementation(libs.com.google.firebase.firebase.analytics.ktx)
    implementation(libs.firebase.perf.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lottie Implementation
    implementation(libs.lottie)

    // In-App Updates
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // Encryption for session management
    implementation(libs.androidx.security.crypto)

    // Root Detection
    implementation(libs.rootbeer)

}