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
        minSdk = 21
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
    implementation(platform(libs.firebase.bom))
    //implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ktx)
    // implementation(libs.androidx.activity)
    // implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.junit)
    // androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)

}