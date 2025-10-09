plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.android_project"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.android_project"
        minSdk = 26
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.google.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //ML kit
    implementation("com.google.mlkit:text-recognition:16.0.1")

    //GSON for json generate
    implementation("com.google.code.gson:gson:2.10.1")


    //Apache POI
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    //NanoHTTPD for HTTP server
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    //Document scanner
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

}
