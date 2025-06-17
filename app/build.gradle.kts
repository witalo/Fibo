plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.apollographql.apollo3") version "3.8.3"
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android") version "2.50"
}
hilt {
    enableAggregatingTask = false
}
android {
    namespace = "com.example.fibo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fibo"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xskip-prerelease-check",
            "-Xexport-kdoc"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.bluetooth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup:javapoet:1.13.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    // Apollo Client
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.3")
    implementation("com.apollographql.apollo3:apollo-api:3.8.3")
    // manejo de operaciones asíncronas y concurrentes en Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Asegúrate de usar la última versión
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Para Android
    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Maps
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")

    // QR Scanner
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Camara personalizada
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    // PDF
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")

    // QR - ZXing (versiones estables)
    implementation("com.google.zxing:core:3.5.1")

    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")

    // Para el escáner de código de barras
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
}

kapt {
    correctErrorTypes = true
}
apollo {
    service("service") {
        packageName.set("com.example.fibo")
        schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
    generateKotlinModels.set(true)
}