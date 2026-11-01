import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.PixelParking.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 67
    versionName = "0.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.play.services.wearable)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

// Ensure latest wear-release.apk is copied into assets
val copyWearReleaseApkToAssets = tasks.register("copyWearReleaseApkToAssets") {
  doLast {
    val candidates = listOf(
      file("${rootDir}/wear-release.apk"),
      file("${rootDir}/wear/build/outputs/apk/release/wear-release.apk")
    )
    val src = candidates.firstOrNull { it.exists() && it.length() > 0 }
    if (src != null) {
      val targetDir = file("src/main/assets")
      targetDir.mkdirs()
      src.copyTo(file("src/main/assets/wear-release.apk"), overwrite = true)
    }
  }
}

tasks.matching { (it.name.startsWith("merge") || it.name.startsWith("generate")) && it.name.endsWith("Assets") }.configureEach {
  dependsOn(copyWearReleaseApkToAssets)
}

val syncAppReleaseApkToRoot = tasks.register("syncAppReleaseApkToRoot") {
  doLast {
    val candidates = listOf(
      file("build/outputs/apk/release/app-release.apk"),
      file("build/intermediates/apk/release/app-release.apk")
    )
    val src = candidates.firstOrNull { it.exists() && it.length() > 0 }
    if (src != null) {
      src.copyTo(file("${rootDir}/PixelParking-release.apk"), overwrite = true)
      src.copyTo(file("${rootDir}/release.apk"), overwrite = true)
      val buildOutputsDir = file("${rootDir}/.build-outputs")
      if (buildOutputsDir.exists()) {
        src.copyTo(file("${rootDir}/.build-outputs/app-release.apk"), overwrite = true)
        src.copyTo(file("${rootDir}/.build-outputs/release.apk"), overwrite = true)
      }
    }
  }
}

val syncAppDebugApkToRoot = tasks.register("syncAppDebugApkToRoot") {
  doLast {
    val candidates = listOf(
      file("build/outputs/apk/debug/app-debug.apk"),
      file("build/intermediates/apk/debug/app-debug.apk")
    )
    val src = candidates.firstOrNull { it.exists() && it.length() > 0 }
    if (src != null) {
      src.copyTo(file("${rootDir}/PixelParking-debug.apk"), overwrite = true)
      src.copyTo(file("${rootDir}/app-debug.apk"), overwrite = true)
      src.copyTo(file("${rootDir}/debug.apk"), overwrite = true)
      val buildOutputsDir = file("${rootDir}/.build-outputs")
      if (buildOutputsDir.exists()) {
        src.copyTo(file("${rootDir}/.build-outputs/app-debug.apk"), overwrite = true)
        src.copyTo(file("${rootDir}/.build-outputs/debug.apk"), overwrite = true)
      }
    }
  }
}

tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }.configureEach {
  finalizedBy(syncAppReleaseApkToRoot)
}

tasks.matching { it.name == "assembleDebug" || it.name == "packageDebug" }.configureEach {
  finalizedBy(syncAppDebugApkToRoot)
}



