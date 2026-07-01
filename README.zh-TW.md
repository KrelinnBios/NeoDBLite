# NeoDBLite

<p align="center">
  <img src="icon/logo.svg" width="128" alt="NeoDB Lite logo">
</p>

<p align="center">
  <strong>NeoDB Lite</strong><br>
  面向 NeoDB 與兼容實例的非官方 Android 標記客戶端
</p>

<p align="center">
  <a href="README.md">简体中文</a> ·
  <a href="README.zh-TW.md">繁體中文</a> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.en.md">English</a>
</p>

## 專案簡介

NeoDB Lite 是面向 [NeoDB](https://neodb.social) 及兼容實例的非官方 Android 客戶端，用於在手機上瀏覽、搜尋和標記書影音遊等條目。

以下說明僅描述 NeoDB Lite 當前實際提供的功能。

## 功能概覽

### 瀏覽 / 發現

- 實例登入：支援填寫 NeoDB 實例域名，並通過 OAuth 授權登入。
- 發現瀏覽：按圖書、電影、劇集、音樂、遊戲、播客和演出查看趨勢內容，網格展示，長按條目可快速標記。
- 條目搜尋：獨立搜尋頁，支援跨類目或限定類目搜尋並分頁載入結果，保留最近搜尋紀錄（可點選、單條刪除、一鍵清空），長按結果同樣支援快速標記。
- 條目詳情：展示封面、標題、評分、簡介、標籤（點擊標籤可跳轉按標籤搜尋）、外部來源連結（豆瓣/IMDb/TMDB/Bangumi 等）和當前帳號的標記狀態。
- 社群內容：在條目詳情中查看公開短評、長評、筆記等社群內容，並可跳轉網頁端查看更多。

### 標記與書架

- 標記編輯：支援設定想讀/在讀/讀過/擱置等書架狀態（按類目顯示對應動詞）、0 到 10 評分、短評、標籤和可見性，可選擇是否同步到聯邦宇宙，也支援修改與刪除標記。
- 我的書架：按書架狀態分頁查看自己的標記，可按類目或標籤篩選（標籤篩選會跨狀態展示該標籤下全部條目），支援按標題關鍵字過濾，並提供日曆檢視查看每日標記分佈。

### 收藏單與個人主頁

- 收藏清單：查看自己的收藏單清單與詳情內條目（目前為唯讀瀏覽，暫不支援建立或編輯收藏單）。
- 個人主頁：展示頭像、簡介、書架統計（點擊可跳轉對應書架）、最近完成條目和收藏單入口。

### 輔助設定

- 主題切換：支援多套主題配色切換。
- 語言切換：支援简体中文、繁體中文、日本語和 English 介面。
- 應用更新：啟動時自動檢查新版本（可在設定頁關閉），支援應用內下載、多來源回退、版本與簽名校驗、喚起系統安裝器、下載失敗重試、手動下載入口，以及問題回饋與登出。

## 介面預覽

<p align="center">
  <img src="screenshots/Screenshot_2026-06-30-18-12-40-43_8d633091d37a6aa.jpg" width="19%" alt="發現頁">
  <img src="screenshots/Screenshot_2026-06-30-18-12-53-95_8d633091d37a6aa.jpg" width="19%" alt="書架">
  <img src="screenshots/Screenshot_2026-06-30-18-13-36-45_8d633091d37a6aa.jpg" width="19%" alt="條目詳情">
  <img src="screenshots/Screenshot_2026-06-30-18-14-24-14_8d633091d37a6aa.jpg" width="19%" alt="個人主頁">
  <img src="screenshots/Screenshot_2026-06-30-18-17-47-14_8d633091d37a6aa.jpg" width="19%" alt="設定">
</p>

## 使用方式

### 安裝使用

從 [Releases](https://github.com/KrelinnBios/NeoDBLite/releases) 下載 APK 後安裝。

### 系統要求

Android 7.0（API 24）及以上。

### 更新方式

應用啟動時會通過 GitHub Releases API 靜默檢查新版本（可在設定頁關閉自動檢查），也可以在設定頁手動檢查更新。檢測到更新後彈出對話框，顯示新版本號和發布說明，提供「手動下載」「稍後」和「下載並安裝」三個選項。

下載過程中顯示進度條、百分比和來源資訊（支援 GitHub 直鏈與鏡像回退）。下載完成後自動校驗 APK 版本與簽名，校驗通過後喚起系統安裝器。若下載或安裝失敗，對話框會顯示錯誤原因並允許重試。

若當前安裝包與 Releases 包簽名不一致，系統安裝器會拒絕覆蓋安裝，需要先解除安裝舊版後重新安裝。

## 技術資訊

- 技術棧：Kotlin、Jetpack Compose、Material 3、Retrofit、OkHttp。
- 授權方式：使用 Mastodon 相容的 OAuth 授權碼流程連接 NeoDB 實例。
- 更新機制：通過 GitHub Releases API 檢查版本，並對下載到的 APK 做版本與簽名校驗。

## 內容邊界

- 本專案為非官方客戶端，與 NeoDB 專案及各實例營運方無隸屬關係。
- 請遵守所登入實例的服務規則、內容規範和所在地法律法規。
- 條目資料、封面圖片、評分、短評和社群內容來自 NeoDB 或對應兼容實例，版權與內容責任歸其原始來源所有。
- 存取令牌僅用於當前應用存取所登入實例；請不要安裝來源不明的改版版本。

## 許可協議

本專案依據 [MIT License](./LICENSE) 發布。

第三方函式庫、平台內容與外部服務以其原作者、專案或服務的許可與使用條款為準。

## 反饋與貢獻

歡迎通過 [GitHub Issue](https://github.com/KrelinnBios/NeoDBLite/issues) 提交使用問題、相容性問題、功能建議或其他改進建議。
