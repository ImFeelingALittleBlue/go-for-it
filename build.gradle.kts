// 根層級 build 檔案：只宣告所有模組會用到的 plugin，不實際套用（apply false）
plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false   // Firebase
    id("com.google.devtools.ksp") version "2.1.10-1.0.31" apply false  // Room 代碼生成
}
