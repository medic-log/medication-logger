plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mediclog.medicationlogger"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mediclog.medicationlogger"
        minSdk = 28
        targetSdk = 37
        versionCode = 7
        versionName = "1.0.0"
        // The public download site, with latest.json and the APK; empty hides Update and Share
        buildConfigField("String", "SITE_URL", "\"https://medic-log.github.io/medication-logger/\"")
        // The Cloudflare Worker that turns feedback into issues
        buildConfigField("String", "FEEDBACK_URL", "\"https://feedback.medic-log.workers.dev\"")
    }

    // Release signing comes from the environment (set by the release workflow); without it the release APK is unsigned
    val releaseKeystore = providers.environmentVariable("SIGNING_KEYSTORE_FILE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("SIGNING_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        // Its own app id, so it installs next to the release; src/debug gives it its own name and icon
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
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
    androidResources {
        // Lists the app's languages in the system's per-app language settings, opened by Language
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions.unitTests {
        // Robolectric runs the unit tests against real Android resources
        isIncludeAndroidResources = true
        // Robolectric's API 37 runtime reaches into FileDescriptor internals, which the JDK hides
        all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Compose — the BOM pins every artifact below it, so none of them carry a version
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Newer than the one ui-test-junit4 brings, which calls an InputManager method that recent Android versions removed
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
