import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")   // Firebase
    id("com.google.devtools.ksp")          // Room 代碼生成
}

android {
    namespace = "com.example.goforit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.goforit"
        minSdk = 26        // Android 8.0，Media3 全功能最低需求
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 從 local.properties 讀取 Mapbox 公開金鑰，注入成 @string/mapbox_access_token
        // resValue：在編譯時動態產生字串資源，不用把金鑰寫死在程式碼裡
        val localProps = Properties().also { props ->
            rootProject.file("local.properties").takeIf { it.exists() }
                ?.inputStream()?.use { props.load(it) }
        }
        resValue("string", "mapbox_access_token",
            localProps.getProperty("MAPBOX_PUBLIC_TOKEN", ""))
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 避免 JPX 的 Java 模組描述檔和其他 library 衝突
            excludes += "META-INF/versions/9/module-info.class"
        }
    }
}

dependencies {
    // ── Jetpack Compose ──────────────────────────────────────────────────────
    // BOM 統一管理所有 Compose 版本，底下的 Compose 依賴不用個別寫版本
    val composeBom = platform("androidx.compose:compose-bom:2025.03.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // 跑步、地圖等進階圖示

    // Compose 整合套件
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // ── 核心 AndroidX ────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // ── Room 本地資料庫 ──────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")   // 讓 Room 支援協程（suspend fun）
    ksp("androidx.room:room-compiler:2.7.1")          // 在編譯時自動生成 SQL 相關程式碼

    // ── Firebase ─────────────────────────────────────────────────────────────
    val firebaseBom = platform("com.google.firebase:firebase-bom:33.13.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-auth-ktx")       // 使用者登入
    implementation("com.google.firebase:firebase-firestore-ktx")  // 雲端資料庫
    implementation("com.google.firebase:firebase-storage-ktx")    // 音檔雲端儲存
    implementation("com.google.firebase:firebase-functions-ktx")  // 呼叫 Claude API 的後端

    // ── Mapbox 地圖 ──────────────────────────────────────────────────────────
    implementation("com.mapbox.maps:android:11.9.0")

    // ── Media3（ExoPlayer）音訊播放 ──────────────────────────────────────────
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1") // 背景播放 + 通知列控制鍵

    // ── JPX：解析使用者匯入的 GPX 路線檔 ────────────────────────────────────
    // 注意：3.x 用了 Java Records 導致 Android 編譯錯誤，改用 2.x
    implementation("io.jenetics:jpx:2.3.0")

    // ── Kotlin 協程（Firebase、Room 的非同步呼叫都靠這個）────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // ── 測試 ─────────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
