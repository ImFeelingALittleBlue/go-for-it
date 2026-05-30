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
| JPX | 2.3.0 | 解析使用者匯入的 GPX 檔（3.x 有 Java Records 相容問題，維持 2.3.0） |
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


## 金鑰設定位置
- **Mapbox Downloads Token**（`sk.` 開頭）：`local.properties` → `MAPBOX_DOWNLOADS_TOKEN`
- **Mapbox Public Token**（`pk.` 開頭）：`local.properties` → `MAPBOX_PUBLIC_TOKEN`（build.gradle.kts 透過 `resValue` 注入，不寫死在 strings.xml）
- **Firebase 設定**：`app/google-services.json`（不提交 git）

> ⚠️ `local.properties` 已加入 `.gitignore`，協作者須自行建立（見 Readme.md）

## 目前狀態（2026-05-30）
- [x] 專案 Gradle 架構建立完成
- [x] 所有依賴設定完成（Compose、Room、Mapbox、Firebase、Media3、JPX 2.3.0）
- [x] Mapbox 兩個金鑰已設定（存於 local.properties，不進 git）
- [x] Firebase google-services.json 已放入
- [x] Gradle sync 成功
- [x] 底部導航列（我的地圖 / 去探索 / 紀錄 / 帳號）
- [x] 首頁 UI：搜尋列 + Mapbox 地圖 + 附近古蹟列表（暫用假資料）
- [x] GPS 定位權限請求 + 地圖藍點（`LocationPermission.kt`）
- [ ] 跑步畫面（去探索）：地圖 + 計時 + 軌跡記錄
- [ ] Room DB：從 metadata.csv 匯入古蹟資料
- [ ] 古蹟觸發：進入 40m 範圍自動播放語音
- [ ] GPX 路線匯入
- [ ] 時光銀鹽積分 + 老照片兌換
- [ ] 使用者登入（Firebase Auth）

## 下一步
**跑步畫面**（去探索）：Mapbox 地圖 + 開始/停止按鈕 + 計時器

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
    │   └── RunScreen.kt               ← 跑步畫面（待實作）
    ├── collection/
    │   └── CollectionScreen.kt        ← 收藏畫面（待實作）
    ├── map/
    │   └── MapScreen.kt               ← 地圖畫面（帳號頁暫用）
    └── theme/
        ├── Color.kt                   ← 臺南風格配色
        └── Theme.kt                   ← GoForItTheme
```
