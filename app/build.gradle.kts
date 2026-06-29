plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun buildValue(name: String, defaultValue: String? = null): String? {
    val value = providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    return value ?: defaultValue
}

val appUpdateUrl = buildValue(
    "APP_UPDATE_URL",
    "https://api.github.com/repos/KrelinnBios/NeoDBLite/releases?per_page=20"
)!!
val appVersionName = buildValue("APP_VERSION_NAME", "1.0.0")!!
val appVersionCode = buildValue("APP_VERSION_CODE", "1")!!.toInt()
val signingKeystorePath = buildValue("ANDROID_KEYSTORE_PATH")
val signingKeyAlias = buildValue("ANDROID_KEY_ALIAS")
val signingKeyPassword = buildValue("ANDROID_KEY_PASSWORD")
val signingStorePassword = buildValue("ANDROID_STORE_PASSWORD")
val hasStableSigning = listOf(
    signingKeystorePath,
    signingKeyAlias,
    signingKeyPassword,
    signingStorePassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.krelinnbios.neodblite"
    compileSdk = 34

    signingConfigs {
        if (hasStableSigning) {
            create("stable") {
                storeFile = file(signingKeystorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.krelinnbios.neodblite"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField(
            "String",
            "APP_UPDATE_URL",
            "\"${appUpdateUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            // 有正式签名材料时用 stable，否则回退到 AGP 自带的 debug 签名，
            // 保证 debug 包始终被签名、可安装；不会因 stable 缺失而被置 null 变成未签名。
            signingConfig = signingConfigs.findByName("stable")
                ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            // release 只用正式签名；stable 缺失时保持 null（CI 会先校验 secrets，本地无密钥则产出未签名包）。
            signingConfig = signingConfigs.findByName("stable")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// 固定 APK 名。AGP 8.13 的公开 VariantOutput 未暴露 outputFileName，故需 cast 到 impl。
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("NeoDB Lite.apk")
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-ktx:1.13.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
