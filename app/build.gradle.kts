plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Ensure this line is present
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.taskmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.taskmanager"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }

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
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf("-Xjvm-default=all", "-Xopt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    kapt {
        correctErrorTypes = true
        useBuildCache = true
    }

    // Enable Parcelize feature
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn" + "-Xopt-in=kotlinx.parcelize.ExperimentalParcelable"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    val roomVersion = "2.5.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation ("androidx.navigation:navigation-ui-ktx:2.5.1")

    // Date and time picker
    implementation("com.google.android.material:material:1.9.0")
    
    // Performance monitoring & analytics
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.benchmark:benchmark-common:1.2.0")
    implementation("androidx.benchmark:benchmark-junit4:1.2.0")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    
    // Charts for task statistics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Better image loading
    implementation("com.github.bumptech.glide:glide:4.15.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.0")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Leak detection in debug builds
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    debugImplementation("com.squareup.leakcanary:plumber-android:2.10")
    
    // Performance monitoring for investigation
    debugImplementation("com.facebook.flipper:flipper:0.187.0")
    debugImplementation("com.facebook.flipper:flipper-network-plugin:0.187.0")
    debugImplementation("com.facebook.soloader:soloader:0.10.5")
}
