plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ua.goworkout"
    compileSdk = 34

    defaultConfig {
        applicationId = "ua.goworkout"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures{
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.volley)
    implementation (libs.glide.v4151)
    implementation (libs.gson)
    implementation (libs.play.services.base)
    implementation(libs.common)
    implementation(libs.okhttp)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.ui.geometry.android)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.junit.junit)

}

