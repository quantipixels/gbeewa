import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "GbeewaShowcase"
            isStatic = true
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(libs.compose.ui.backhandler)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.decompose)
        implementation(libs.decompose.compose)
        implementation(libs.navigation.compose)
        implementation(libs.voyager.navigator)
        implementation(projects.gbeewa)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.activity.compose)
    }
}

android {
    namespace = "com.quantipixels.gbeewa.showcase"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.quantipixels.gbeewa.showcase"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.compile.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
