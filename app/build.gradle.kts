plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.ideality"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ideality"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dataBinding {
        enable = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // Add this line
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/notice.txt",
                "META-INF/license.txt",
                "META-INF/CHANGES",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Individual Firebase dependencies without versions
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // AndroidX and other dependencies
    implementation(libs.androidx.recyclerview)
    implementation(libs.filament.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.core)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.flexbox)
    implementation(libs.arsceneview)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.io.github.sceneview.sceneview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.volley)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Other direct dependencies
    implementation("com.hbb20:ccp:2.7.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.google.firebase:firebase-config-ktx")
    implementation ("com.google.android.material:material:1.11.0")
    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ARCore dependencies
    implementation ("com.google.ar:core:1.14.0")

    //Chip Navigation
    implementation ("com.github.ibrahimsn98:NiceBottomBar:2.2")

    //auth
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    
    //material3
    implementation("androidx.compose.material3:material3")
    implementation(platform("androidx.compose:compose-bom:2023.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    //lottie
    implementation ("com.airbnb.android:lottie:6.6.0")

    //searchview
    implementation ("com.paulrybitskyi.persistentsearchview:persistentsearchview:1.1.5")

    //recycle view
    implementation ("jp.wasabeef:recyclerview-animators:4.0.2")

    // Add Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    //
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("jp.wasabeef:recyclerview-animators:4.0.2")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    //appwrite
    implementation("io.appwrite:sdk-for-android:6.0.0")

    // For email sending
    implementation ("com.sun.mail:android-mail:1.6.7")
    implementation ("com.sun.mail:android-activation:1.6.7")

    // For secure storage of credentials
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")

    // Image loading and caching
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Image cropping
    implementation ("com.github.yalantis:ucrop:2.2.8")

    // Circle ImageView
    implementation ("de.hdodenhof:circleimageview:3.1.0")


}
