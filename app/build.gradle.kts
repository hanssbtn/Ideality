plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")

    // Existing plugins
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
}


android {
    namespace = "com.example.ideality"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ideality"
        minSdk = 29
        targetSdk = 35
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.volley)

    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    // Jetpack Compose + Material3
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)
    // Jetpack Compose Preview
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    // Jetpack Compose UI test
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Jetpack Compose Add full set of material icons
    implementation(libs.androidx.material.icons.extended)
    // Jetpack Compose Add window size utils
    implementation(libs.androidx.adaptive)
    // Jetpack Compose Integration with activities
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Jetpack Compose Integration with LiveData
    implementation(libs.androidx.runtime.livedata)
    // ConstraintLayout
    implementation(libs.androidx.constraintlayout.compose)

    // ARCore
    implementation(libs.core)
    // ARSceneView
    implementation(libs.arsceneview)

    // Other direct dependencies
    implementation("com.hbb20:ccp:2.7.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation ("com.google.firebase:firebase-config-ktx")
    implementation ("com.google.android.material:material:1.11.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)
    // Jetpack Compose BOM
    implementation(libs.androidx.material3)
    // Jetpack Compose Preview
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    // Jetpack Compose UI test
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Jetpack Compose Add full set of material icons
    implementation(libs.androidx.material.icons.extended)
    // Jetpack Compose Add window size utils
    implementation(libs.androidx.adaptive)
    // Jetpack Compose Integration with activities
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Jetpack Compose Integration with LiveData
    implementation(libs.androidx.runtime.livedata)
    // ConstraintLayout
    implementation(libs.androidx.constraintlayout.compose)

    // ARCore
    implementation(libs.core)
    // SceneView
    implementation(libs.sceneview)
    // ARSceneView
    implementation(libs.arsceneview)

    // Filament
    implementation(libs.filament.android)

    // Google Services
    implementation(libs.google.services)

    // Firebase BOM
    //noinspection UseTomlInstead
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    // Firebase Core
    implementation(libs.firebase.core)
    // Firebase Auth
    implementation(libs.firebase.auth.ktx)
    // FirebaseUI Auth
    implementation(libs.androidx.credentials.play.services.auth)
    // Firebase Firestore
    implementation(libs.firebase.firestore.ktx)
    // Firebase Analytics
    implementation(libs.firebase.analytics.ktx)
    // Firebase Database
    implementation(libs.firebase.database.ktx)

    // Coroutines
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Navigation
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(libs.androidx.navigation.testing)

    // AndroidX KTX
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.core.ktx)

    // Chip Navigation
    implementation ("com.github.ibrahimsn98:NiceBottomBar:2.2")

    // auth
    implementation("com.google.android.recaptcha:recaptcha:18.6.1")
    implementation("com.google.android.gms:play-services-safetynet:18.1.0")

    //material3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")

    // Lottie
    implementation ("com.airbnb.android:lottie:6.6.0")

    // SearchView
    implementation ("com.paulrybitskyi.persistentsearchview:persistentsearchview:1.1.5")

    // RecycleView Animators
    implementation ("jp.wasabeef:recyclerview-animators:4.0.2")
    
    //
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Appwrite
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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
