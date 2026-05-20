plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kronos.olympusfront"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kronos.olympusfront"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Retrofit pour l'API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ViewPager2 (navigation horizontale par swipe)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    
    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Barcode scanning (ZXing)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}