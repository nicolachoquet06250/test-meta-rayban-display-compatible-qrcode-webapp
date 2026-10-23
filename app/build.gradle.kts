plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val signingKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val signingKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val signingKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    signingKeystorePath,
    signingKeystorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.example.metaqr"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.metaqr"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["mwdat_application_id"] = providers.gradleProperty("mwdat_application_id").orElse("0").get()
        manifestPlaceholders["mwdat_client_token"] = providers.gradleProperty("mwdat_client_token").orElse("0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("ciRelease") {
                storeFile = file(requireNotNull(signingKeystorePath))
                storePassword = signingKeystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.meta.wearable:mwdat-core:1.0.0")
    implementation("com.meta.wearable:mwdat-camera:1.0.0")
    implementation("com.meta.wearable:mwdat-display:1.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    testImplementation("junit:junit:4.13.2")
}
