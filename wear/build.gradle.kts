plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.PixelParking.wear"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.PixelParking.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 46
    versionName = "0.7"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
    release {
      signingConfig = signingConfigs.getByName("debugConfig")
      isMinifyEnabled = false
      isDebuggable = false
    }
  }

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.wear)
  implementation(libs.androidx.wear.compose.material)
  implementation(libs.androidx.wear.compose.foundation)
  implementation(libs.androidx.wear.compose.navigation)
  implementation(libs.play.services.wearable)
  implementation(libs.play.services.location)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
}

val syncWearReleaseApkToRoot = tasks.register("syncWearReleaseApkToRoot") {
  doLast {
    val candidates = listOf(
      file("build/outputs/apk/release/wear-release.apk"),
      file("build/intermediates/apk/release/wear-release-unsigned.apk"),
      file("build/intermediates/apk/release/wear-release.apk")
    )
    val src = candidates.firstOrNull { it.exists() && it.length() > 0 }
    if (src != null) {
      src.copyTo(file("${rootDir}/wear-release.apk"), overwrite = true)
    }
  }
}

tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }.configureEach {
  finalizedBy(syncWearReleaseApkToRoot)
}
