plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.parcelize")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
    mapDiagnosticLocations = true
    javacOptions {
        option("-Xmaxerrs", 1000)
    }
    arguments {
        arg("room.incremental", "true")
    }
}

android {
    namespace = "com.topjohnwu.magisk"

    defaultConfig {
        applicationId = "io.github.huskydg.magisk"
        vectorDrawables.useSupportLibrary = true
        versionName = Config.version
        versionCode = Config.versionCode
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules.pro")
        }
    }

    ndkVersion = "26.1.10909125"

    buildFeatures {
        dataBinding = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/*"
            excludes += "/META-INF/versions/**"
            excludes += "/org/bouncycastle/**"
            excludes += "/kotlin/**"
            excludes += "/kotlinx/**"
            excludes += "/okhttp3/**"
            excludes += "/*.txt"
            excludes += "/*.bin"
            excludes += "/*.json"
        }
    }
}

setupApp()

configurations.all {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
    implementation(project(":app:shared"))

    implementation("com.github.topjohnwu:jtar:1.0.0")
    implementation("com.github.topjohnwu:indeterminate-checkbox:1.0.7")
    implementation("com.github.topjohnwu:lz4-java:1.7.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")
    implementation("dev.rikka.rikkax.insets:insets:1.3.0")
    implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.3.2")
    implementation("io.noties.markwon:core:4.6.2")

    val vLibsu = "5.2.2"
    implementation("com.github.topjohnwu.libsu:core:${vLibsu}")
    implementation("com.github.topjohnwu.libsu:service:${vLibsu}")
    implementation("com.github.topjohnwu.libsu:nio:${vLibsu}")

    val vRetrofit = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:${vRetrofit}")
    implementation("com.squareup.retrofit2:converter-moshi:${vRetrofit}")
    implementation("com.squareup.retrofit2:converter-scalars:${vRetrofit}")

    val vOkHttp = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:${vOkHttp}")
    implementation("com.squareup.okhttp3:logging-interceptor:${vOkHttp}")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:${vOkHttp}")

    val vMoshi = "1.15.0"
    implementation("com.squareup.moshi:moshi:${vMoshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${vMoshi}")

    val vRoom = "2.6.1"
    implementation("androidx.room:room-runtime:${vRoom}")
    implementation("androidx.room:room-ktx:${vRoom}")
    kapt("androidx.room:room-compiler:${vRoom}")

    val vNav = "2.7.6"
    implementation("androidx.navigation:navigation-fragment-ktx:${vNav}")
    implementation("androidx.navigation:navigation-ui-ktx:${vNav}")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.transition:transition:1.4.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("com.google.android.material:material:1.11.0")
}
