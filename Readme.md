# 臺南古蹟重建跑步 App

學校作業。使用者匯入 GPX 路線，邊跑步邊聽古蹟語音導覽，靠近古蹟自動播放，並收集「時光銀鹽」兌換老照片。

Figma 設計稿：https://www.figma.com/design/DvrclsHl9R2hLEpDAoHhdk/Untitled?node-id=1-633&t=itjR5wbXNJbg8W5g-1

---

## 技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| Kotlin + Jetpack Compose | 2.1.10 / BOM 2025.03.00 | 主要語言 + UI |
| Room | 2.7.1 | 本地資料庫 |
| Mapbox Maps | 11.9.0 | 地圖導航 |
| Firebase | BOM 33.13.0 | Auth + Firestore + Storage + Functions |
| Media3 (ExoPlayer) | 1.5.1 | 語音播放 |
| JPX | 2.3.0 | GPX 路線解析 |

---

## 目前進度

- [x] Gradle 專案架構與所有依賴設定
- [x] 底部導航列（我的地圖 / 去探索 / 紀錄 / 帳號）
- [x] 首頁 UI：搜尋列 + Mapbox 地圖 + 附近古蹟列表
- [x] GPS 定位權限請求 + 地圖藍點顯示
- [ ] 跑步畫面：Mapbox 地圖 + 計時 + 軌跡記錄
- [ ] Room DB：從 metadata.csv 匯入 130+ 筆古蹟資料
- [ ] 古蹟觸發：進入 40m 範圍自動播放語音
- [ ] GPX 路線匯入
- [ ] 時光銀鹽：積分收集 + 老照片兌換
- [ ] 使用者登入（Firebase Auth）

---

## 環境設定（協作者必讀）

Clone 之後需要手動補兩個不在 git 裡的檔案：

### 1. `local.properties`（放在根目錄，從頭建立）

```properties
sdk.dir=你的Android SDK路徑

# Mapbox Secret Token（有 DOWNLOADS:READ 權限，sk. 開頭）
MAPBOX_DOWNLOADS_TOKEN=sk.eyJ1...

# Mapbox Public Token（pk. 開頭，從 Default public token 複製）
MAPBOX_PUBLIC_TOKEN=pk.eyJ1...
```

到 [account.mapbox.com/access-tokens](https://account.mapbox.com/access-tokens/) 取得這兩個金鑰。

### 2. `app/google-services.json`

從 Firebase Console 下載，放到 `app/` 資料夾下。

---

## 古蹟資料

`metadata.csv` — 130+ 筆臺南古蹟，含 lat/lng、描述、老照片欄位。
