plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.salmansaleem.edusphere"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salmansaleem.edusphere"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database:21.0.0")


    implementation("com.google.firebase:firebase-functions:20.4.0")
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")


    implementation(libs.volley)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(libs.androidx.exifinterface)

    implementation ("com.squareup.picasso:picasso:2.71828")


    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.jsonwebtoken:jjwt:0.12.6")

}

apply(plugin = "com.google.gms.google-services") // 🔥 Important!