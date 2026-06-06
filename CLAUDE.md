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

> ⚠️ `local.properties` 已加入 `.gitignore`，協作者須自行建立（見 Readme.md）

## 目前狀態（2026-06-06）
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
- [x] **紀錄頁面**（`CollectionScreen.kt`）：跑完自動儲存 + 愛心收藏 + 刪除按鈕
- [x] **已儲存路線**（`SavedRoutesScreen.kt`）：只顯示按愛心的紀錄，點擊直接載入原始 GPX 重跑
- [x] GPX 原始內容隨紀錄存進 Firestore，按愛心後不需重新上傳即可再次選路線跑步
- [ ] Room DB：從 metadata.csv 匯入古蹟資料
- [ ] 古蹟觸發：進入 40m 範圍自動播放語音
- [ ] AI 語音導覽：Firebase Functions + Claude API 生成旁白
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
    │   ├── RunScreen.kt               ← 去探索主畫面（地圖 + 計時 + 軌跡）
    │   ├── RunTracker.kt              ← GPS 軌跡記錄（LocationManager）
    │   ├── RunningOverlay.kt          ← 跑步中共用元件（統計列、解鎖通知、對話框）
    │   ├── RouteRunningPanel.kt       ← 選擇路線跑步中的頂部/底部面板
    │   ├── RunSummaryScreen.kt        ← 跑步結算頁（地圖 + 統計 + 命名 + 分享）
    │   ├── SavedRoutesScreen.kt       ← 已儲存路線（愛心收藏清單 + GPX 上傳）
    │   ├── GpxParser.kt               ← GPX 解析（XmlPullParser + Haversine）
    │   └── RoutePreviewScreen.kt      ← 路線預覽 + 開始跑步
    ├── collection/
    │   └── CollectionScreen.kt        ← 紀錄頁（跑步紀錄 + 愛心 + 刪除）
    ├── map/
    │   └── MapScreen.kt               ← 地圖畫面（帳號頁暫用）
    └── theme/
        ├── Color.kt                   ← 臺南風格配色
        └── Theme.kt                   ← GoForItTheme
```
