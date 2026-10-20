import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing: on Codemagic the keystore comes from CM_KEYSTORE_* variables
// (android_signing reference "BeеWarrior"); locally from keystore.properties
// (storeFile, storePassword, keyAlias, keyPassword). Without either, bundleRelease
// produces an unsigned .aab.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}
val cmKeystorePath = System.getenv("CM_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
fun signingValue(key: String, cmEnv: String): String? =
    (if (cmKeystorePath != null) System.getenv(cmEnv) else keystoreProperties.getProperty(key))
        ?.takeIf { it.isNotBlank() }
val releaseStoreFile = cmKeystorePath ?: signingValue("storeFile", "CM_KEYSTORE_PATH")
val hasReleaseSigning = releaseStoreFile?.let { rootProject.file(it).exists() } == true
require(cmKeystorePath == null || hasReleaseSigning) { "Codemagic keystore file is unavailable" }

android {
    namespace = "com.katadojo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.starfoodbv.katadojo"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.4"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "CM_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "CM_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "CM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
