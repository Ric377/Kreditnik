plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")                 // Room
}

android {
    namespace  = "com.kreditnik.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kreditnik.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"
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
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }

    /* ⬇︎  указываем версию компилятора для Compose 1.8/1.9 */
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }  // совместимо с BOM-2025-05-01
}

kapt {
    arguments { arg("room.schemaLocation", "$projectDir/schemas") }
}

dependencies {

    /* ───── Jetpack Compose (BOM) ───── */
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))          /* 20 мая 2025 :contentReference[oaicite:0]{index=0} */
    implementation("androidx.compose.material3:material3")                       /* 1.3.2 через BOM – содержит SmallTopAppBar :contentReference[oaicite:1]{index=1} */
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")          // базовый UI-модуль
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    /* ───── Навигация Compose ───── */
    implementation("androidx.navigation:navigation-compose:2.9.0")               /* стабильный релиз 7 мая 2025  */

    /* ───── Lifecycle / ViewModel ───── */
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    /* ───── Room 2.7 (апр 2025) ───── */
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation("androidx.room:room-paging:2.7.1")

    /* ───── Core / Activity ───── */
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    /* ───── Material Components v1 (виджеты View) ───── */
    implementation("com.google.android.material:material:1.12.0")

    /* ───── Тесты ───── */
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
