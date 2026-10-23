plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
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
