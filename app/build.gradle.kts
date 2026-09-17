import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Build-time configuration: gradle.properties, overridable by local.properties.
val local = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun setting(name: String, default: String = ""): String =
    (local.getProperty(name) ?: project.findProperty(name)?.toString() ?: default).trim()

// Release signing: environment variables first (CI, from secrets), then a
// keystore file on disk (a developer's machine). Neither is required — a
// release build with no signing config configured here is simply unsigned,
// as it was before.
val keystorePath = System.getenv("BOOKISHAM_KEYSTORE_PATH") ?: setting("bookisham.keystore.path", "release.keystore.jks")
val keystoreFile = rootProject.file(keystorePath).let { if (it.isAbsolute) it else file(keystorePath) }
val hasSigning = keystoreFile.exists() || System.getenv("BOOKISHAM_KEYSTORE_PATH") != null

android {
    namespace = "com.bookisham.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bookisham.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        buildConfigField("String", "BASE_URL", "\"${setting("bookisham.baseUrl", "https://bookisham.example.com")}\"")
        buildConfigField("String", "WHATSAPP", "\"${setting("bookisham.whatsapp")}\"")
        buildConfigField("String", "CONTACT_EMAIL", "\"${setting("bookisham.contactEmail")}\"")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("BOOKISHAM_KEYSTORE_PASSWORD") ?: setting("bookisham.keystore.password", "bookisham-release")
                keyAlias = System.getenv("BOOKISHAM_KEY_ALIAS") ?: setting("bookisham.key.alias", "bookisham")
                keyPassword = System.getenv("BOOKISHAM_KEY_PASSWORD") ?: setting("bookisham.key.password", "bookisham-release")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
