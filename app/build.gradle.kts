plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val ciDebugKeystorePath = System.getenv("DEBUG_KEYSTORE_PATH")
val ciDebugKeystoreFile = ciDebugKeystorePath?.let { file(it) }
val useCiDebugKeystore = ciDebugKeystoreFile != null && ciDebugKeystoreFile.exists()

android {
    namespace = "net.matsudamper.normalize_share_image"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.matsudamper.normalize_share_image"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (useCiDebugKeystore) {
            create("debugCi") {
                storeFile = ciDebugKeystoreFile
                storePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (useCiDebugKeystore) {
                signingConfig = signingConfigs.getByName("debugCi")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}