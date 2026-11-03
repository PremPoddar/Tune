import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing is read from a git-ignored keystore.properties (see keystore.properties.sample).
// Until it's filled in, release builds are signed with the debug key so they still install.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f -> FileInputStream(f).use { load(it) } }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile")?.let { file(it).exists() } == true &&
    keystoreProps.getProperty("storePassword").let { !it.isNullOrBlank() && it != "CHANGE_ME" }

android {
    namespace = "com.prempoddar.tune"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.prempoddar.tune"
        minSdk = 29
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Reproducible: F-Droid rebuilds this and checks it matches the APK on GitHub byte for
            // byte, so leave out anything that depends on the machine (PNG crunching, git state).
            isCrunchPngs = false
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed with the upload key when keystore.properties is there; otherwise left unsigned,
            // for builders such as F-Droid that sign with their own key.
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        // Optimised like release but without shrinking, for trying builds on a phone: as fast
        // as release, and nothing can be stripped by mistake.
        create("fast") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += listOf("release")
        }
    }

    // Google's encrypted dependency list in the APK's signing block can't be reproduced or read;
    // Play still gets it from the bundle.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Baseline profiles can come out slightly different from one build to the next, which F-Droid's
// byte-for-byte check can't allow, so APKs go without them; the Play bundle keeps them.
tasks.configureEach {
    if (name.contains("ArtProfile") && gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }) enabled = false
}

dependencies {
    implementation(libs.material.color.utilities)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.palette)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
}
