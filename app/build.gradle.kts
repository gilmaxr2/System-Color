plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.systemcolor.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.systemcolor.app"
        minSdk = 30
        targetSdk = 35
        versionCode = 5
        versionName = "5.0-beta"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
