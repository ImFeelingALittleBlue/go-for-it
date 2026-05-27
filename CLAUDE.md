# 臺南古蹟重建跑步 App — 專案說明

## 關於這個專案
學校作業，不上架。讓使用者匯入 GPX 路線、邊跑步邊聽古蹟語音導覽、靠近古蹟自動播放，並收集「時光銀鹽」兌換老照片。

Figma 設計稿：https://www.figma.com/design/DvrclsHl9R2hLEpDAoHhdk/Untitled?node-id=1-633&t=itjR5wbXNJbg8W5g-1

## 關於開發者
- 程式新手，學過一點 Kotlin，第一次做 Android App
- 報告要用，所以程式碼需要加註解說明每段在做什麼
- **每次只做一個小功能，做完確認跑起來才繼續**
- **單一檔案不超過 200 行**
- 遇到不熟悉的概念，先簡短解釋再寫程式

## 技術棧與版本

| 技術 | 版本 | 用途 |
|------|------|------|
| AGP | 8.9.2 | Android Gradle Plugin |
| Kotlin | 2.1.10 | 主要語言 |
| Compose BOM | 2025.03.00 | UI 框架 |
| Room | 2.7.1 | 本地資料庫（古蹟資料、跑步記錄） |
| Mapbox Maps | 11.9.0 | 地圖導航 |
| Firebase BOM | 33.13.0 | Auth + Firestore + Storage + Functions |
| Media3 | 1.5.1 | 音訊播放（語音導覽） |
| JPX | 3.2.0 | 解析使用者匯入的 GPX 檔 |
| KSP | 2.1.10-1.0.31 | Room 代碼生成（編譯時） |

## Package 名稱
`com.example.goforit`

## 功能規格（依優先順序）
1. **GPX 匯入** → 後端（Firebase Functions + Claude API）生成路線旁白 → Google TTS 轉音檔
2. **跑步畫面** → Mapbox 地圖導航 + 位置追蹤（ForegroundService）
3. **古蹟觸發** → 進入古蹟 40m 範圍自動播放對應段落（Media3）
4. **時光銀鹽** → 收集古蹟積分，兌換老照片重建（2D 疊圖）
5. **使用者登入** → Firebase Auth

## 古蹟資料
`metadata.csv` — 130+ 筆臺南古蹟，含 lat/lng/描述/老照片欄位

## 專案目錄結構
```
C:\go_for_it\
├── CLAUDE.md                          ← 本檔案
├── Readme.md                          ← 專案背景
├── metadata.csv                       ← 130+ 古蹟資料
├── build.gradle.kts                   ← 根層級 Gradle（宣告 plugin 版本）
├── settings.gradle.kts                ← 模組設定 + Mapbox Maven 倉庫
├── gradle.properties                  ← Gradle 設定 + MAPBOX_DOWNLOADS_TOKEN
├── local.properties                   ← Android SDK 路徑（不提交 git）
├── gradlew.bat                        ← Windows Gradle 執行腳本
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties      ← Gradle 8.11.1
└── app/
    ├── build.gradle.kts               ← 所有依賴在這裡
    ├── google-services.json           ← Firebase 設定（不提交 git）
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── kotlin/com/example/goforit/
        │   ├── MainActivity.kt        ← 入口，目前只顯示啟動成功文字
        │   └── ui/theme/
        │       ├── Color.kt           ← 臺南風格配色（磚紅/沙黃/古銅金）
        │       └── Theme.kt           ← GoForItTheme Compose 主題
        └── res/values/
            ├── strings.xml            ← app_name + mapbox_access_token（pk.）
            └── themes.xml             ← Activity 視窗主題（NoActionBar）
```

## 金鑰設定位置
- **Mapbox Downloads Token**（`sk.` 開頭）：`gradle.properties` 第 22 行
- **Mapbox Public Token**（`pk.` 開頭）：`app/src/main/res/values/strings.xml`
- **Firebase 設定**：`app/google-services.json`（已放入）

## 目前狀態（2026-05-27）
- [x] 專案 Gradle 架構建立完成
- [x] 所有依賴設定完成（Compose、Room、Mapbox、Firebase、Media3、JPX）
- [x] Mapbox 兩個金鑰已設定
- [x] Firebase google-services.json 已放入
- [x] MainActivity 可以跑起來（顯示啟動文字）
- [x] Gradle sync 成功
- [ ] 功能尚未實作

## 下一步
第一個功能：**底部導航列 + 各頁面骨架**（首頁、跑步、地圖、收藏）
