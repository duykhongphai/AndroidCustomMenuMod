plugins {
    id("com.android.library")
}

android {
    namespace = "com.nguyen.onyxpayload"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(project(":onyx-core"))
    testImplementation("junit:junit:4.13.2")
}
