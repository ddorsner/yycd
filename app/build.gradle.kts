plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dabbled.wordpressnewsletter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dabbled.wordpressnewsletter"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(project(":shared"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.core.ktx)

    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    implementation(libs.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso)
}