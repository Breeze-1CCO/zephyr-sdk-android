plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  id("maven-publish")
}

android {
  namespace = "in.breeze.blaze"
  compileSdk = 34

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
}


afterEvaluate {
  publishing {
    publications {
      create<MavenPublication>("maven") {
        groupId = "in.breeze"
        artifactId = "blaze"
        version = "0.0.1-alpha"
        artifact(layout.buildDirectory.file("outputs/aar/blaze-release.aar"))
      }
    }
  }
  tasks.named("publishMavenPublicationToMavenLocal").configure {
    dependsOn("bundleReleaseAar")
  }
}