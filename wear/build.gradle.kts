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
    versionCode = 67
    versionName = "0.7"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: "uploadpassword"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "uploadpassword"
      enableV1Signing = true
      enableV2Signing = true
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    release {
      initWith(getByName("debug"))
      isDebuggable = false
      signingConfig = signingConfigs.getByName("debugConfig")
      isMinifyEnabled = false
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
      val dest = file("${rootDir}/wear-release.apk")
      src.copyTo(dest, overwrite = true)
      dest.setReadable(true, false)
      val buildOutputsDir = file("${rootDir}/.build-outputs")
      if (buildOutputsDir.exists()) {
        val destOut = file("${rootDir}/.build-outputs/wear-release.apk")
        src.copyTo(destOut, overwrite = true)
        destOut.setReadable(true, false)
      }
    }
  }
}

val syncWearDebugApkToRoot = tasks.register("syncWearDebugApkToRoot") {
  doLast {
    val candidates = listOf(
      file("build/outputs/apk/debug/wear-debug.apk"),
      file("build/intermediates/apk/debug/wear-debug.apk")
    )
    val src = candidates.firstOrNull { it.exists() && it.length() > 0 }
    if (src != null) {
      val dest = file("${rootDir}/wear-debug.apk")
      src.copyTo(dest, overwrite = true)
      dest.setReadable(true, false)
      val buildOutputsDir = file("${rootDir}/.build-outputs")
      if (buildOutputsDir.exists()) {
        val destOut = file("${rootDir}/.build-outputs/wear-debug.apk")
        src.copyTo(destOut, overwrite = true)
        destOut.setReadable(true, false)
      }
    }
  }
}

tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }.configureEach {
  finalizedBy(syncWearReleaseApkToRoot)
}

tasks.matching { it.name == "assembleDebug" || it.name == "packageDebug" }.configureEach {
  finalizedBy(syncWearDebugApkToRoot)
}
