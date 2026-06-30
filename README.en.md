# NeoDBLite

<p align="center">
  <img src="docs/icon.svg" width="128" alt="NeoDB Lite logo">
</p>

<p align="center">
  <strong>NeoDB Lite</strong><br>
  An unofficial Android bookmarking client for NeoDB and compatible instances
</p>

<p align="center">
  <a href="README.md">简体中文</a> ·
  <a href="README.zh-TW.md">繁體中文</a> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.en.md">English</a>
</p>

## About

NeoDB Lite is an unofficial Android client for [NeoDB](https://neodb.social) and compatible instances, allowing you to browse, search, and bookmark books, films, TV series, music, games, podcasts, and performances on your phone.

## Features

### Browse / Discover

- Instance login: enter your NeoDB instance domain and authenticate via OAuth.
- Discover feed: browse trending content by category — books, films, TV, music, games, podcasts, and performances.
- Item search: search across all categories or filter by a specific category with paginated results.
- Item details: view cover, title, rating, description, tags, and your current bookmark status.
- Community content: read public comments, reviews, and notes on item detail pages, with a link to the web version for more.

### Bookmark Management

- Bookmarking: set shelf status (want to read / reading / read / dropped), 0–10 rating, short comment, and visibility; supports editing and deleting bookmarks.
- My shelf: browse your bookmarks by shelf status with category filtering, and a calendar view for review.
- Profile page: view account info, shelf overview, recently completed items, and common settings.

### Preferences

- Theme switching: multiple color themes to choose from.
- Language switching: interface available in 简体中文, 繁體中文, 日本語, and English.
- App updates: automatically checks for new versions on startup (can be disabled in settings), supports in-app download, system installer integration, download retry, and manual download.

## Screenshots

<p align="center">
  <img src="screenshots/Screenshot_2026-06-30-18-12-40-43_8d633091d37a6aa.jpg" width="19%" alt="Discover">
  <img src="screenshots/Screenshot_2026-06-30-18-12-53-95_8d633091d37a6aa.jpg" width="19%" alt="Shelf">
  <img src="screenshots/Screenshot_2026-06-30-18-13-36-45_8d633091d37a6aa.jpg" width="19%" alt="Item detail">
  <img src="screenshots/Screenshot_2026-06-30-18-14-24-14_8d633091d37a6aa.jpg" width="19%" alt="Profile">
  <img src="screenshots/Screenshot_2026-06-30-18-17-47-14_8d633091d37a6aa.jpg" width="19%" alt="Settings">
</p>

## Usage

### Installation

Download the APK from [Releases](https://github.com/KrelinnBios/NeoDBLite/releases) and install it.

### Requirements

Android 7.0 (API 24) or later.

### Updates

The app silently checks for new versions via the GitHub Releases API on startup (auto-check can be disabled in settings). You can also manually check for updates in the settings page. When an update is found, a dialog shows the new version and release notes, offering three options: "Manual Download", "Later", and "Download & Install".

The download progress bar shows percentage and source info (supports GitHub direct link and mirror fallback). After download, the APK version and signature are verified before launching the system installer. If download or installation fails, the dialog displays the error and allows a retry.

## Technical Details

- Tech stack: Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp.
- Auth: Mastodon-compatible OAuth authorization code flow to connect to NeoDB instances.
- Updates: version check via GitHub Releases API, with APK version and signature verification on downloaded packages.

## Disclaimer

- This is an unofficial client and is not affiliated with the NeoDB project or any instance operators.
- Please abide by the rules, content policies, and applicable laws of your instance.
- Item data, cover images, ratings, comments, and community content are sourced from NeoDB or compatible instances; copyright and content responsibility belong to their original sources.
- Access tokens are used solely for accessing your instance from this app. Do not install modified builds from untrusted sources.

## License

This project is released under the [MIT License](./LICENSE).

Third-party libraries, platform content, and external services are governed by the licenses and terms of their respective authors, projects, or services.

## Feedback & Contributions

Issues, feature requests, and suggestions are welcome via [GitHub Issues](https://github.com/KrelinnBios/NeoDBLite/issues).
