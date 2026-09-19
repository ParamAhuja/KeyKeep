plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Add this line to enable symbol processing
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

android {
    namespace = "com.example.keykeep"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.keykeep"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom)) // declared at ln.50
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
//    Added:
    val roomVersion = "2.8.5" // Updated from 2.6.1
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
//    kapt("androidx.room:room-compiler:$roomVersion")

// SQLCipher Community Edition
    implementation("net.zetetic:sqlcipher-android:4.19.0@aar")
    implementation("androidx.sqlite:sqlite:2.7.1") // Updated from 2.7.0
}