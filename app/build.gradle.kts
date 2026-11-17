plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.room")
}

android {
    namespace = "com.eqnitymusic.damn"
    compileSdk = 34
    buildToolsVersion = "35.0.1"

    defaultConfig {
        applicationId = "com.eqnitymusic.damn"
        minSdk = 26
        targetSdk = 34
        versionCode = 67
        versionName = "6.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    namespace = "com.eqnitymusic.damn"

    room {
        schemaDirectory("$projectDir/schemas")
    }

    // Exclude problematic transitive dependencies causing duplicate classes
    configurations {
        matching { it.name.startsWith("debugRuntime") || it.name.startsWith("releaseRuntime") || it.name.startsWith("androidTestRuntime") || it.name.startsWith("testRuntime") }.all {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-util-klib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-util-io")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-gradle-plugin-api")
            exclude(group = "com.android.tools", module = "common")
            exclude(group = "com.android.tools.build", module = "bundletool")
            exclude(group = "com.sun.activation", module = "javax.activation")
            exclude(group = "xmlpull", module = "xmlpull")
            exclude(group = "xpp3", module = "xpp3")
            exclude(group = "xml-apis", module = "xml-apis")
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Media playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")

    // Room Database (Room Gradle plugin handles KSP)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Hilt Dependency Injection - Use 2.50 for Kotlin 1.9.x compatibility (KSP)
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Retrofit & OkHttp for network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading (Glide - KSP)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // Palette for dynamic colors from album art
    implementation("androidx.palette:palette-ktx:1.0.0")

    // MediaStore for storage
    implementation("androidx.media:media:1.6.0")

    // WorkManager for background downloads
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.50")
}