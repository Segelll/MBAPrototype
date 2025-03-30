plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.mbaprototype"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mbaprototype"
        minSdk = 31
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
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.core:core-ktx:1.12.0") // Or newer
    implementation("androidx.appcompat:appcompat:1.6.1") // Or newer
    implementation("com.google.android.material:material:1.11.0") // Or newer
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Or newer
    // Add these lines:

    // Lifecycle components (ViewModel, LiveData, LifecycleScope, repeatOnLifecycle)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0") // Provides viewModelScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")  // Provides lifecycleScope, repeatOnLifecycle
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0") // Optional but often used

    // Activity KTX (provides viewModels delegate for Activities)
    implementation("androidx.activity:activity-ktx:1.9.0") // Or newer

    // Fragment KTX (provides activityViewModels delegate for Fragments)
    implementation("androidx.fragment:fragment-ktx:1.7.0") // Or newer
}