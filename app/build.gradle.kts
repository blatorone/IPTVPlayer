plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.iptvplayer.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.iptvplayer.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 113
        versionName = "1.0.99-beta.1"

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
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    implementation(libs.zxing.core)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
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
}

kotlin {
    compilerOptions {
        // The main screen's lambdas capture a very large number of states. Compiled as
        // invokedynamic methods (Kotlin 2 default) every capture becomes a parameter, and
        // methods with ~250 registers were rejected by the ART verifier on Android 11
        // ("type Conflict unexpected as arg to if-eqz"). Class-based lambdas keep the
        // captures in fields.
        freeCompilerArgs.add("-Xlambdas=class")
    }
}
