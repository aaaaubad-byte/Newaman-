import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

// AMAN Supabase client configuration is supplied from an ignored local properties file
// (or Gradle properties/environment in CI). No service_role/database secret is accepted.
val amanSupabaseProps = Properties().apply {
    val f = rootProject.file("supabase.local.properties")
    if (f.exists()) f.inputStream().use { input -> load(input) }
}
val amanSupabaseUrl = (amanSupabaseProps.getProperty("SUPABASE_URL") ?: project.findProperty("SUPABASE_URL")?.toString() ?: System.getenv("SUPABASE_URL") ?: "").trim()
val amanSupabaseAnonKey = (amanSupabaseProps.getProperty("SUPABASE_ANON_KEY") ?: project.findProperty("SUPABASE_ANON_KEY")?.toString() ?: System.getenv("SUPABASE_ANON_KEY") ?: "").trim()
if (amanSupabaseAnonKey.contains("service_role", ignoreCase = true)) error("service_role key must never be packaged in AMAN Android")

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aman.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "SUPABASE_URL", "\"${amanSupabaseUrl.replace("\"", "\\\"")}\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"${amanSupabaseAnonKey.replace("\"", "\\\"")}\"")
  }
  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
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

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  testImplementation(libs.junit)
}

// Keep this file free of credentials; production values must come from CI/local properties.
