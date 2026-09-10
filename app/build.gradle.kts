plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.spotpal.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.spotpal.app"
        minSdk = 26          // Android 8.0+（详细设计 §1.1）
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // 模拟器 10.0.2.2 直达宿主机服务端（详细设计 §6 联调）
            buildConfigField("String", "API_BASE", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "WS_BASE", "\"ws://10.0.2.2:8080/ws\"")
        }
        release {
            isMinifyEnabled = true          // R8 全量混淆
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE", "\"https://api.spotpal.cn\"")
            buildConfigField("String", "WS_BASE", "\"wss://api.spotpal.cn/ws\"")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:negotiate"))
    implementation(project(":feature:confirm"))
    implementation(project(":feature:squads"))
    implementation(project(":feature:me"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
