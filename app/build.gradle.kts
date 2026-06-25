import com.android.build.api.dsl.ApplicationExtension
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appName = project.property("app.name") as String
val appVersionName = project.property("app.versionName") as String
val appVersionCode = project.property("app.versionCode") as String

base { archivesName.set("afinity-v${appVersionName}") }

configure<ApplicationExtension> {
    namespace = "com.makd.afinity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.makd.afinity"
        minSdk = 35
        targetSdk = 36
        versionCode = appVersionCode.toInt()
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APP_NAME", "\"${appName}\"")
        buildConfigField("String", "VERSION_NAME", "\"${appVersionName}\"")
        buildConfigField("int", "VERSION_CODE", appVersionCode)
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("boolean", "DEBUG", "true")
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
            excludes += "/META-INF/INDEX.LIST"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources { generateLocaleConfig = true }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xjvm-default=all",
            "-Xcontext-parameters",
        )
    }
}

dependencies {
    implementation(project(":shared"))

    // Android entry point only — the whole app UI lives in :shared (Compose Multiplatform).
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.timber)
    coreLibraryDesugaring(libs.android.desugar.jdk)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
