plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.kreditnik.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kreditnik.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.10"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    /* ───── Jetpack Compose (BOM) ───── */
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.foundation:foundation:1.6.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material:1.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    /* ───── Навигация Compose ───── */
    implementation("androidx.navigation:navigation-compose:2.9.0")

    /* ───── Lifecycle / ViewModel ───── */
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    /* ───── Room ───── */
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation("androidx.room:room-paging:2.7.1")

    /* ───── Core / Activity ───── */
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    /* ───── Material Components ───── */
    implementation("com.google.android.material:material:1.12.0")

    /* ───── Datastore ───── */
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    /* ───── Accompanist Pager для HorizontalPager ───── */
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    implementation ("androidx.compose.material3:material3:<latest_version>")

    // WorkManager
    implementation ("androidx.work:work-runtime-ktx:2.7.1") // Проверьте актуальную версию WorkManager
    // Kotlinx Datetime (если еще не используется, но у вас, кажется, уже есть)
    implementation ("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0") // Проверьте актуальную версию

    /* ───── Тесты ───── */
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
