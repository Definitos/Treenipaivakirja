plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "fi.ville.treenipaivakirja"
    compileSdk = 35

    defaultConfig {
        applicationId = "fi.ville.treenipaivakirja"
        minSdk = 26
        targetSdk = 35
        // CI:n ajonumero kasvattaa versiota, jotta päivitys asentuu vanhan päälle
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = run + 1
        versionName = "1.$run"
    }

    // Kiinteä allekirjoitusavain: muuten jokainen CI-käännös saisi eri avaimen
    // eikä päivitys asentuisi vanhan päälle.
    signingConfigs {
        create("treeni") {
            storeFile = file("treeni.jks")
            storePassword = "treeni123"
            keyAlias = "treeni"
            keyPassword = "treeni123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("treeni")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("treeni")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
