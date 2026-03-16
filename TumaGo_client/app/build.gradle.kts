import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.techmania.tumago"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
    }

    packagingOptions {
        exclude ("META-INF/NOTICE.md")
        exclude ("META-INF/NOTICE")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/NOTICE.md")
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
    }

    // --- Fix 1: Release signing config from local.properties ---
    signingConfigs {
        create("release") {
            storeFile = file(localProperties["KEYSTORE_FILE"]?.toString() ?: "keystore.jks")
            storePassword = localProperties["KEYSTORE_PASSWORD"]?.toString() ?: ""
            keyAlias = localProperties["KEY_ALIAS"]?.toString() ?: ""
            keyPassword = localProperties["KEY_PASSWORD"]?.toString() ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.techmania.tumago"
        minSdk = 24
        targetSdk = 35

        // --- Fix 3: Read version from local.properties (default 1 / "1.0") ---
        versionCode = (localProperties["VERSION_CODE"]?.toString()?.toIntOrNull()) ?: 1
        versionName = localProperties["VERSION_NAME"]?.toString() ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties["MAPS_API_KEY"]}\"")

        manifestPlaceholders["MAPS_API_KEY"] = localProperties["MAPS_API_KEY"] ?: ""
    }

    // --- Fix 2: Build flavors (dev / staging / prod) ---
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL",
                "\"${localProperties["DEV_BASE_URL"] ?: localProperties["BASE_URL"] ?: "http://10.0.2.2"}\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "BASE_URL",
                "\"${localProperties["STAGING_BASE_URL"] ?: "https://staging.tumago.co.zw"}\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL",
                "\"${localProperties["PROD_BASE_URL"] ?: "https://api.tumago.co.zw"}\"")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // --- Fix 5: Lint configuration ---
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.2.0")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.firebase:firebase-messaging:23.1.2")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
