import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

// Secrets/config are read from local.properties (gitignored) so they never need to
// be committed to source. Falls back to the previous hardcoded values when a key
// isn't present, so the project still builds out of the box; override these in
// local.properties (or inject via CI env vars into that file) for real deployments.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

fun localOrDefault(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

val backendBaseUrlRelease = localOrDefault("BACKEND_BASE_URL_RELEASE", "https://connect-backend-e22a.onrender.com/graphql")
val backendBaseUrlDebug = localOrDefault("BACKEND_BASE_URL_DEBUG", backendBaseUrlRelease)
val razorpayKeyIdRelease = localOrDefault("RAZORPAY_KEY_ID_RELEASE", "rzp_test_SDi8IlcjLgcYQE")
val razorpayKeyIdDebug = localOrDefault("RAZORPAY_KEY_ID_DEBUG", razorpayKeyIdRelease)

android {
    namespace = "np.com.bimalkafle.firebaseauthdemoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "np.com.bimalkafle.firebaseauthdemoapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrlDebug\"")
            buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKeyIdDebug\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrlRelease\"")
            buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKeyIdRelease\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.razorpay:checkout:1.6.40")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
