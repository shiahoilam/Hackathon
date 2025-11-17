plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.hackathon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hackathon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.0")
    }
    exclude(group = "com.android.support", module = "support-compat")
    exclude(group = "com.android.support", module = "support-annotations")
    exclude(group = "com.android.support", module = "support-core-utils")
    exclude(group = "com.android.support", module = "support-v4")

    // 🚫 get rid of *all* old Google Sceneform artifacts (1.17.1)
    exclude(group = "com.google.ar.sceneform")
}

//
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    implementation("com.google.ar:core:1.33.0")

    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    // TFLite runtime
    implementation ("org.tensorflow:tensorflow-lite:2.13.0")
    // Optional GPU delegate
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.13.0")
    // Optional support library for metadata, image processing
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.3")

//    implementation ("com.google.ar.sceneform:core:1.17.1'")

//    // Sceneform with exclusions
//    implementation("com.google.ar.sceneform:core:1.17.1") {
//        exclude(group = "com.android.support")
//    }
//    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1") {
//        exclude(group = "com.android.support")
//    }

    // https://mvnrepository.com/artifact/com.google.ar.sceneform/plugin
//    implementation("com.google.ar.sceneform:plugin:1.17.1")
//    implementation ("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")

//    implementation ("com.google.ar.sceneform:core:1.17.1")
//    implementation ("com.google.ar:core:1.41.0")
//    implementation ("com.gorisse.thomas.sceneform:sceneform:1.23.0")


    implementation ("com.gorisse.thomas.sceneform:sceneform:1.23.0")


}




