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
| KSP | 2.1.10-1.0.31 | Room 代碼生成（編譯時） |

> ⚠️ JPX 已移除：JPX 底層使用 `javax.xml.stream.XMLInputFactory`，Android 上不存在此類別會直接 crash。GPX 解析改用 Android 內建的 `XmlPullParser`（見 `GpxParser.kt`）。

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


## 金鑰設定位置
- **Mapbox Downloads Token**（`sk.` 開頭）：`local.properties` → `MAPBOX_DOWNLOADS_TOKEN`
- **Mapbox Public Token**（`pk.` 開頭）：`local.properties` → `MAPBOX_PUBLIC_TOKEN`（build.gradle.kts 透過 `resValue` 注入，不寫死在 strings.xml）
- **Firebase 設定**：`app/google-services.json`（不提交 git）
- **Anthropic API Key**：`local.properties` → `ANTHROPIC_API_KEY`（build.gradle.kts 透過 `buildConfigField` 注入為 `BuildConfig.ANTHROPIC_API_KEY`）

> ⚠️ `local.properties` 已加入 `.gitignore`，協作者須自行建立（見 Readme.md）

## 目前狀態（2026-06-14）
- [x] 專案 Gradle 架構建立完成
- [x] 所有依賴設定完成（Compose、Room、Mapbox、Firebase、Media3）
- [x] Mapbox 兩個金鑰已設定（存於 local.properties，不進 git）
- [x] Firebase google-services.json 已放入
- [x] Gradle sync 成功
- [x] 底部導航列（我的地圖 / 去探索 / 紀錄 / 帳號）
- [x] 首頁 UI：搜尋列 + Mapbox 地圖 + 附近古蹟列表（暫用假資料）
- [x] GPS 定位權限請求 + 地圖藍點（`LocationPermission.kt`）
- [x] 跑步畫面（去探索1）：地圖 + 古蹟標記 + 計時器 + GPS 軌跡折線 + 開始/停止
- [x] 已儲存路線畫面（去探索2）：按愛心的路線清單 + GPX 臨時上傳按鈕
- [x] GPX 路線匯入：XmlPullParser 解析 + Haversine 距離計算
- [x] 路線預覽畫面（去探索3）：地圖折線 + 距離徽章 + AI 導覽佔位符 + 開始跑步
- [x] **選擇路線跑步中 UI**（`RouteRunningPanel.kt`）：時光銀鹽頂部列 + 地圖 + AI Podcast 卡（進度點依路線古蹟排列）+ 最近古蹟 + 暫停按鈕
- [x] **直接跑步中 UI**：暫停按下跳出「要結束旅程嗎？」對話框（繼續跑步 / 結束跑步）
- [x] **跑步結算頁**：自動判斷地區命名（台南中西區探索）+ 右側鉛筆改名 + 日期/地區副標
- [x] **紀錄頁面**（`CollectionScreen.kt`）：月份分組卡片 + 愛心收藏 + 點擊查看詳情
- [x] **已儲存路線**（`SavedRoutesScreen.kt`）：只顯示按愛心的紀錄，點擊直接載入原始 GPX 重跑
- [x] GPX 原始內容隨紀錄存進 Firestore，按愛心後不需重新上傳即可再次選路線跑步
- [x] **紀錄詳情頁**（`RecordDetailScreen.kt`）：地圖折線 + 統計 + 再跑一次（PendingRunStore）+ 分享
- [x] **分享底頁**（`ShareSheet.kt`）：Mapbox Snapshotter 截圖（含真實地圖底圖）+ 儲存圖片 + AI Podcast 佔位
- [x] **Podcast 卡片 UI 重設計**（`RouteRunningPanel.kt`）：垂直古蹟列表、可捲動（LazyColumn）、播放中高亮、點卡片重播、⏸ 暫停鍵
- [x] **Podcast 自動依序播放**：進入路線跑步立刻播第一站，播完自動播下一站，使用者暫停才停
- [x] **規劃路線限定點選古蹟**（`RoutePlanScreen.kt`）：地圖只能點 80m 內的古蹟標點，空白處忽略
- [x] **地圖古蹟名稱提示框**（`HeritageTooltip.kt`）：點選古蹟標點浮出名稱小標籤，2 秒自動消失（`RoutePlanScreen`、`RunScreen`）
- [x] **Claude API Podcast 生成**（`PodcastGenerator.kt`）：呼叫 `claude-haiku-4-5` 生成客製化兩人對話腳本，風格像《故事 FM》直切故事核心
- [x] **預生成快取**（`PodcastCache.kt`）：「開始跑步」前先 call API 存快取，進場即刻播放零等待；WAV 下載與跑步播放使用同一份腳本
- [x] **Podcast 時長輸入改為自由輸入分鐘數**（`StoryState.kt`）：輸入總時長，系統自動除以古蹟數分攤每站
- [x] Anthropic API Key 從 `local.properties` 讀入，透過 `BuildConfig.ANTHROPIC_API_KEY` 注入
- [ ] Room DB：從 metadata.csv 匯入古蹟資料
- [ ] 古蹟觸發：進入 40m 範圍自動播放語音
- [ ] 時光銀鹽積分 + 老照片兌換
- [ ] 使用者登入（Firebase Auth）

## 下一步
**Room DB**：從 `metadata.csv` 匯入 130+ 筆古蹟資料，供地圖標記與古蹟觸發使用

## 專案目錄結構
```
app/src/main/kotlin/com/example/goforit/
├── MainActivity.kt                    ← 入口，Scaffold + BottomNavBar + NavHost
├── navigation/
│   ├── Screen.kt                      ← 頁面路由定義（sealed class）
│   └── BottomNavBar.kt                ← 底部導航列元件
└── ui/
    ├── home/
    │   ├── HomeScreen.kt              ← 首頁（搜尋列 + 地圖 + 古蹟列表）
    │   ├── HeritageSection.kt         ← 附近古蹟列表區塊
    │   └── LocationPermission.kt      ← GPS 權限請求邏輯
    ├── run/
    │   ├── RunScreen.kt               ← 去探索主畫面（地圖 + 計時 + 軌跡 + 古蹟tooltip）
    │   ├── RunTracker.kt              ← GPS 軌跡記錄（LocationManager）
    │   ├── RunningOverlay.kt          ← 跑步中共用元件（統計列、解鎖通知、對話框）
    │   ├── RouteRunningPanel.kt       ← 選擇路線跑步中的頂部/底部面板（Podcast卡片列表）
    │   ├── RoutePlanScreen.kt         ← 規劃路線（只能點選古蹟標點 + tooltip 2秒消失）
    │   ├── RunSummaryScreen.kt        ← 跑步結算頁（地圖 + 統計 + 命名 + 分享）
    │   ├── RecordDetailScreen.kt      ← 紀錄詳情頁（從紀錄頁點入，含地圖+統計+再跑一次）
    │   ├── ShareSheet.kt              ← 分享底頁（Mapbox Snapshotter 截圖 + 下載WAV）
    │   ├── MapSnapshot.kt             ← Mapbox Snapshotter 工具（生成含底圖的路線圖片）
    │   ├── ShareUtils.kt              ← 分享工具（Canvas 路線圖、存相簿、存 GPX）
    │   ├── HeritageTooltip.kt         ← 古蹟名稱提示框（點選地圖標點 → 2秒後自動消失）
    │   ├── PodcastPlayer.kt           ← TTS 播放器（快取優先 → API → 模板 fallback）
    │   ├── PodcastGenerator.kt        ← Claude API 生成 Podcast 腳本（Haiku 模型）
    │   ├── PodcastCache.kt            ← 預生成快取（開始跑步前存好，跑步中即刻播放）
    │   ├── PodcastExporter.kt         ← TTS → WAV 匯出（使用快取腳本）
    │   ├── StoryState.kt              ← Podcast 時長狀態（整段時長 ÷ 古蹟數）
    │   ├── SavedRoutesScreen.kt       ← 已儲存路線（愛心收藏清單 + GPX 上傳）
    │   ├── GpxParser.kt               ← GPX 解析（XmlPullParser + Haversine）
    │   └── RoutePreviewScreen.kt      ← 路線預覽 + 開始跑步（預生成 Podcast 腳本）
    ├── collection/
    │   └── CollectionScreen.kt        ← 紀錄頁（跑步紀錄 + 愛心 + 刪除）
    ├── map/
    │   └── MapScreen.kt               ← 地圖畫面（帳號頁暫用）
    └── theme/
        ├── Color.kt                   ← 臺南風格配色
        └── Theme.kt                   ← GoForItTheme
```
