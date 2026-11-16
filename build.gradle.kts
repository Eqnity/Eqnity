// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Force single versions of problematic transitive dependencies
            force("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.9.22")
            force("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22")
            force("org.jetbrains.kotlin:kotlin-util-klib:1.9.22")
            force("org.jetbrains.kotlin:kotlin-util-io:1.9.22")
            force("org.jetbrains.kotlin:kotlin-android-extensions:1.9.22")
            // Use jakarta.activation instead of javax.activation
            force("jakarta.activation:jakarta.activation-api:2.1.3")
            // Use kxml2 for xmlpull
            force("net.sf.kxml:kxml2:2.3.0")
        }
    }
}