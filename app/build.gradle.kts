plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.esmanhoto.leantv"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.esmanhoto.leantv"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so `adb install` works out of the box.
            // Replace with your own signing config if you distribute builds.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        textReport = true
    }
}

dependencies {
    // Runtime: none. Pure Android framework + Kotlin stdlib, by design (RAM/APK budget).
    testImplementation("junit:junit:4.13.2")
    // Real org.json for JVM unit tests (the android.jar one is stubbed out)
    testImplementation("org.json:json:20240303")
}
