plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.keronei.kmlguage"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.keronei.kmlguage"
        minSdk = 16
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        missingDimensionStrategy("device", "anyDevice")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        enable = true
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

    namespace = "com.keronei.kmlgauge"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(project(":usbSerialForAndroid"))
    implementation("com.google.firebase:firebase-analytics:17.0.0")
    implementation("com.google.firebase:firebase-crashlytics:17.0.0")
    // implementation(libs.androidx.activity)
    // implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.junit)
    // androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

}