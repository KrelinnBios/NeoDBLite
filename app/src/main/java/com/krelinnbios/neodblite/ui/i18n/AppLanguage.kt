package com.krelinnbios.neodblite.ui.i18n

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.krelinnbios.neodblite.ui.theme.AppTheme
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import java.util.Locale

enum class AppLanguage(val label: String) {
    SYSTEM("跟随系统"),
    ZH_HANS("简体中文"),
    ZH_HANT("繁體中文"),
    JA("日本語"),
    EN("English");

    companion object {
        val DEFAULT = SYSTEM

        fun fromName(name: String?): AppLanguage =
            entries.firstOrNull { it.name == name } ?: DEFAULT

        fun resolve(language: AppLanguage): AppLanguage {
            if (language != SYSTEM) return language
            val tag = Locale.getDefault().toLanguageTag().lowercase(Locale.ROOT)
            return when {
                tag.startsWith("zh-hant") || tag.contains("-tw") || tag.contains("-hk") || tag.contains("-mo") -> ZH_HANT
                tag.startsWith("zh") -> ZH_HANS
                tag.startsWith("ja") -> JA
                else -> EN
            }
        }
    }
}

object AppLanguagePreference {
    private const val PREFS_NAME = "neodb_ui"
    private const val KEY_LANGUAGE = "app_language"

    fun load(context: Context): AppLanguage {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return AppLanguage.fromName(name)
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }
}

data class AppStrings(
    val navSettings: String,
    val language: String,
    val themeColor: String,
    val all: String,
    val searchScope: String,
    val searchPlaceholder: String,
    val search: String,
    val searchToStart: String,
    val noSearchResults: String,
    val recentSearches: String,
    val collections: String,
    val itemsCount: String,
    val noContent: String,
    val loading: String,
    val loadingMore: String,
    val retry: String,
    val noRating: String,
    val unnamedItem: String,
    val networkError: String,
    val myRating: String,
    val loginSubtitle: String,
    val instanceHost: String,
    val login: String,
    val loginHint: String,
    val detail: String,
    val back: String,
    val peopleCountSuffix: String,
    val mark: String,
    val editMark: String,
    val intro: String,
    val sources: String,
    val externalLink: String,
    val openInBrowser: String,
    val status: String,
    val rating: String,
    val unrated: String,
    val visibility: String,
    val shortCommentOptional: String,
    val tagsOptional: String,
    val syncToFediverse: String,
    val saveMark: String,
    val deleteMark: String,
    val saved: String,
    val markDeleted: String,
    val communityContent: String,
    val comments: String,
    val reviews: String,
    val notes: String,
    val openCommunityInBrowser: String,
    val noCommunityContent: String,
    val communityLoadFailed: String,
    val homepage: String,
    val username: String,
    val displayName: String,
    val openHomepage: String,
    val shelfOverview: String,
    val totalMarks: String,
    val shelfSynced: String,
    val recentComplete: String,
    val noCompleteItems: String,
    val checkUpdate: String,
    val checking: String,
    val currentVersionPrefix: String,
    val alreadyLatest: String,
    val releasesPage: String,
    val releasesSubtitle: String,
    val appTagline: String,
    val logoutTitle: String,
    val logoutMessage: String,
    val logout: String,
    val cancel: String,
    val notLoggedIn: String,
    val newVersionPrefix: String,
    val downloadFailedPrefix: String,
    val unknownError: String,
    val downloading: String,
    val downloadAndInstall: String,
    val goReleases: String,
    val sourceLabelPrefix: String,
    val feedback: String,
    val autoUpdateCheck: String,
    val autoUpdateCheckSubtitle: String,
    val preparingDownload: String,
    val downloadingAndVerifying: String,
    val sourcePrefix: String,
    val installerOpened: String,
    val updateFailed: String,
    val targetVersionPrefix: String,
    val newVersionReleased: String,
    val manualDownload: String,
    val later: String,
    val retryAutoUpdate: String,
    val reopenInstaller: String,
    val checkUpdateFailed: String,
    val openDownloadPage: String,
    val openDownloadPageHint: String,
    val themeBlueBlack: String,
    val themeTealLight: String,
    val themeTealDark: String,
    val themeSakura: String,
    val themePurple: String,
    val close: String,
    val bio: String,
    val noBio: String
) {
    fun categoryLabel(category: Category?): String {
        val c = category ?: return ""
        return when (this) {
            ZH_HANS -> c.zhHans
            ZH_HANT -> c.zhHant
            JA -> c.ja
            EN -> c.en
            else -> c.zhHans
        }
    }

    fun shelfLabel(shelf: ShelfType, category: Category?): String = when (this) {
        ZH_HANS -> shelfLabelZhHans(shelf, category)
        ZH_HANT -> shelfLabelZhHant(shelf, category)
        JA -> shelfLabelJa(shelf, category)
        EN -> shelfLabelEn(shelf, category)
        else -> shelfLabelZhHans(shelf, category)
    }

    fun visibilityLabel(visibility: Visibility): String = when (this) {
        ZH_HANS -> visibility.zhHans
        ZH_HANT -> visibility.zhHant
        JA -> visibility.ja
        EN -> visibility.en
        else -> visibility.zhHans
    }

    fun languageLabel(language: AppLanguage): String = when (language) {
        AppLanguage.SYSTEM -> this.languageSystem
        AppLanguage.ZH_HANS -> "简体中文"
        AppLanguage.ZH_HANT -> "繁體中文"
        AppLanguage.JA -> "日本語"
        AppLanguage.EN -> "English"
    }

    fun themeLabel(theme: AppTheme): String = when (theme) {
        AppTheme.BLUE_BLACK -> themeBlueBlack
        AppTheme.TEAL_LIGHT -> themeTealLight
        AppTheme.TEAL_DARK -> themeTealDark
        AppTheme.SAKURA -> themeSakura
        AppTheme.MIDNIGHT_PURPLE -> themePurple
    }

    val languageSystem: String
        get() = when (this) {
            ZH_HANS -> "跟随系统"
            ZH_HANT -> "跟隨系統"
            JA -> "システムに合わせる"
            EN -> "System"
            else -> "跟随系统"
        }

    private fun shelfLabelZhHans(shelf: ShelfType, category: Category?): String {
        val verbs = when (category) {
            Category.BOOK -> Triple("想读", "在读", "读过")
            Category.MOVIE, Category.TV, Category.PERFORMANCE -> Triple("想看", "在看", "看过")
            Category.MUSIC, Category.PODCAST -> Triple("想听", "在听", "听过")
            Category.GAME -> Triple("想玩", "在玩", "玩过")
            null -> Triple("想要", "进行中", "已完成")
        }
        return when (shelf) {
            ShelfType.WISHLIST -> verbs.first
            ShelfType.PROGRESS -> verbs.second
            ShelfType.COMPLETE -> verbs.third
            ShelfType.DROPPED -> "搁置"
        }
    }

    private fun shelfLabelZhHant(shelf: ShelfType, category: Category?): String {
        val verbs = when (category) {
            Category.BOOK -> Triple("想讀", "在讀", "讀過")
            Category.MOVIE, Category.TV, Category.PERFORMANCE -> Triple("想看", "在看", "看過")
            Category.MUSIC, Category.PODCAST -> Triple("想聽", "在聽", "聽過")
            Category.GAME -> Triple("想玩", "在玩", "玩過")
            null -> Triple("想要", "進行中", "已完成")
        }
        return when (shelf) {
            ShelfType.WISHLIST -> verbs.first
            ShelfType.PROGRESS -> verbs.second
            ShelfType.COMPLETE -> verbs.third
            ShelfType.DROPPED -> "擱置"
        }
    }

    private fun shelfLabelJa(shelf: ShelfType, category: Category?): String = when (shelf) {
        ShelfType.WISHLIST -> when (category) {
            Category.BOOK -> "読みたい"
            Category.MUSIC, Category.PODCAST -> "聴きたい"
            Category.GAME -> "遊びたい"
            else -> "見たい"
        }
        ShelfType.PROGRESS -> when (category) {
            Category.BOOK -> "読んでいる"
            Category.MUSIC, Category.PODCAST -> "聴いている"
            Category.GAME -> "遊んでいる"
            else -> "見ている"
        }
        ShelfType.COMPLETE -> when (category) {
            Category.BOOK -> "読了"
            Category.MUSIC, Category.PODCAST -> "聴いた"
            Category.GAME -> "遊んだ"
            else -> "見た"
        }
        ShelfType.DROPPED -> "中断"
    }

    private fun shelfLabelEn(shelf: ShelfType, category: Category?): String = when (shelf) {
        ShelfType.WISHLIST -> when (category) {
            Category.BOOK -> "Want to read"
            Category.MUSIC, Category.PODCAST -> "Want to listen"
            Category.GAME -> "Want to play"
            else -> "Want to watch"
        }
        ShelfType.PROGRESS -> when (category) {
            Category.BOOK -> "Reading"
            Category.MUSIC, Category.PODCAST -> "Listening"
            Category.GAME -> "Playing"
            else -> "Watching"
        }
        ShelfType.COMPLETE -> when (category) {
            Category.BOOK -> "Read"
            Category.MUSIC, Category.PODCAST -> "Listened"
            Category.GAME -> "Played"
            else -> "Watched"
        }
        ShelfType.DROPPED -> "Dropped"
    }
}

private val Category.zhHans: String get() = when (this) {
    Category.BOOK -> "图书"
    Category.MOVIE -> "电影"
    Category.TV -> "剧集"
    Category.MUSIC -> "音乐"
    Category.GAME -> "游戏"
    Category.PODCAST -> "播客"
    Category.PERFORMANCE -> "演出"
}

private val Category.zhHant: String get() = when (this) {
    Category.BOOK -> "圖書"
    Category.MOVIE -> "電影"
    Category.TV -> "劇集"
    Category.MUSIC -> "音樂"
    Category.GAME -> "遊戲"
    Category.PODCAST -> "播客"
    Category.PERFORMANCE -> "演出"
}

private val Category.ja: String get() = when (this) {
    Category.BOOK -> "本"
    Category.MOVIE -> "映画"
    Category.TV -> "ドラマ"
    Category.MUSIC -> "音楽"
    Category.GAME -> "ゲーム"
    Category.PODCAST -> "ポッドキャスト"
    Category.PERFORMANCE -> "公演"
}

private val Category.en: String get() = when (this) {
    Category.BOOK -> "Books"
    Category.MOVIE -> "Movies"
    Category.TV -> "TV"
    Category.MUSIC -> "Music"
    Category.GAME -> "Games"
    Category.PODCAST -> "Podcasts"
    Category.PERFORMANCE -> "Performances"
}

private val Visibility.zhHans: String get() = when (this) {
    Visibility.PUBLIC -> "公开"
    Visibility.FOLLOWERS -> "仅关注者"
    Visibility.PRIVATE -> "仅自己"
}

private val Visibility.zhHant: String get() = when (this) {
    Visibility.PUBLIC -> "公開"
    Visibility.FOLLOWERS -> "僅關注者"
    Visibility.PRIVATE -> "僅自己"
}

private val Visibility.ja: String get() = when (this) {
    Visibility.PUBLIC -> "公開"
    Visibility.FOLLOWERS -> "フォロワーのみ"
    Visibility.PRIVATE -> "自分のみ"
}

private val Visibility.en: String get() = when (this) {
    Visibility.PUBLIC -> "Public"
    Visibility.FOLLOWERS -> "Followers only"
    Visibility.PRIVATE -> "Private"
}

val ZH_HANS = AppStrings(
    navSettings = "设置",
    language = "语言",
    themeColor = "主题颜色",
    all = "全部",
    searchScope = "选择搜索范围",
    searchPlaceholder = "输入名字搜索",
    search = "搜索",
    searchToStart = "按搜索键开始搜索",
    noSearchResults = "没有找到相关条目",
    recentSearches = "最近搜索",
    collections = "我的合集",
    itemsCount = "条目",
    noContent = "暂无内容",
    loading = "加载中…",
    loadingMore = "加载中…",
    retry = "重试",
    noRating = "暂无评分",
    unnamedItem = "未命名条目",
    networkError = "网络异常，请稍后重试",
    myRating = "我的评分",
    loginSubtitle = "登录你的 NeoDB 实例，开始标记书影音游",
    instanceHost = "实例域名",
    login = "登录",
    loginHint = "将在应用内打开实例授权页，授权后自动完成登录。NeoDB 是去中心化平台，默认实例为 neodb.social，也可填写其它兼容实例。",
    detail = "详情",
    back = "返回",
    peopleCountSuffix = " 人",
    mark = "标记",
    editMark = "修改标记",
    intro = "简介",
    sources = "来源",
    externalLink = "外部链接",
    openInBrowser = "在浏览器中打开",
    status = "状态",
    rating = "评分",
    unrated = "未评分",
    visibility = "可见性",
    shortCommentOptional = "短评（可选）",
    tagsOptional = "标签（空格或逗号分隔，可选）",
    syncToFediverse = "同步到联邦宇宙",
    saveMark = "保存标记",
    deleteMark = "删除标记",
    saved = "已保存",
    markDeleted = "已删除标记",
    communityContent = "社区内容",
    comments = "短评",
    reviews = "长评",
    notes = "笔记",
    openCommunityInBrowser = "打开网页端查看全部",
    noCommunityContent = "暂无公开内容",
    communityLoadFailed = "社区内容加载失败",
    homepage = "主页",
    username = "用户名",
    displayName = "显示名",
    openHomepage = "在浏览器中打开主页",
    shelfOverview = "书架概览",
    totalMarks = "累计标记",
    shelfSynced = "已同步当前账号书架数据",
    recentComplete = "最近完成",
    noCompleteItems = "还没有完成的条目",
    checkUpdate = "检查更新",
    checking = "检查中…",
    currentVersionPrefix = "当前版本 v",
    alreadyLatest = "已是最新版本",
    releasesPage = "Releases 页面",
    releasesSubtitle = "在浏览器中查看历史版本",
    appTagline = "NeoDB Lite · 非官方 NeoDB 客户端",
    logoutTitle = "退出登录",
    logoutMessage = "将清除本地保存的登录令牌，需要重新授权才能继续使用。",
    logout = "退出账号",
    cancel = "取消",
    notLoggedIn = "未登录",
    newVersionPrefix = "发现新版本 v",
    downloadFailedPrefix = "下载失败：",
    unknownError = "未知错误",
    downloading = "下载中…",
    downloadAndInstall = "下载并安装",
    goReleases = "前往 Releases",
    sourceLabelPrefix = "源",
    feedback = "问题反馈",
    autoUpdateCheck = "自动版本更新",
    autoUpdateCheckSubtitle = "软件启动时自动检查新版本",
    preparingDownload = "正在准备下载…",
    downloadingAndVerifying = "正在下载并校验 APK",
    sourcePrefix = "来源：",
    installerOpened = "系统安装器已打开。若安装失败，可返回此处选择手动下载。",
    updateFailed = "自动更新失败：",
    targetVersionPrefix = "目标版本：",
    newVersionReleased = "新版本已经发布。",
    manualDownload = "手动下载",
    later = "稍后",
    retryAutoUpdate = "重试自动更新",
    reopenInstaller = "重新打开安装器",
    checkUpdateFailed = "自动检查更新失败",
    openDownloadPage = "打开下载页",
    openDownloadPageHint = "可以前往 GitHub Releases 手动检查并下载最新版本。",
    themeBlueBlack = "蓝黑",
    themeTealLight = "海青·浅",
    themeTealDark = "墨绿·深",
    themeSakura = "樱粉·浅",
    themePurple = "暮紫·深",
    close = "关闭",
    bio = "个人简介",
    noBio = "暂无简介"
)

val ZH_HANT = ZH_HANS.copy(
    navSettings = "設定",
    language = "語言",
    themeColor = "主題顏色",
    all = "全部",
    searchScope = "選擇搜尋範圍",
    searchPlaceholder = "輸入名稱搜尋",
    search = "搜尋",
    searchToStart = "按搜尋鍵開始搜尋",
    noSearchResults = "找不到相關條目",
    recentSearches = "最近搜尋",
    collections = "我的合集",
    itemsCount = "條目",
    noContent = "暫無內容",
    loading = "載入中…",
    loadingMore = "載入中…",
    retry = "重試",
    noRating = "暫無評分",
    unnamedItem = "未命名條目",
    networkError = "網路異常，請稍後再試",
    myRating = "我的評分",
    loginSubtitle = "登入你的 NeoDB 實例，開始標記書影音遊",
    instanceHost = "實例網域",
    login = "登入",
    loginHint = "將在應用內開啟實例授權頁，授權後自動完成登入。NeoDB 是去中心化平台，預設實例為 neodb.social，也可填寫其他相容實例。",
    detail = "詳情",
    back = "返回",
    peopleCountSuffix = " 人",
    mark = "標記",
    editMark = "修改標記",
    intro = "簡介",
    sources = "來源",
    externalLink = "外部連結",
    openInBrowser = "在瀏覽器中開啟",
    status = "狀態",
    rating = "評分",
    unrated = "未評分",
    visibility = "可見性",
    shortCommentOptional = "短評（可選）",
    tagsOptional = "標籤（空格或逗號分隔，可選）",
    syncToFediverse = "同步到聯邦宇宙",
    saveMark = "儲存標記",
    deleteMark = "刪除標記",
    saved = "已儲存",
    markDeleted = "已刪除標記",
    communityContent = "社群內容",
    comments = "短評",
    reviews = "長評",
    notes = "筆記",
    openCommunityInBrowser = "開啟網頁端查看全部",
    noCommunityContent = "暫無公開內容",
    communityLoadFailed = "社群內容載入失敗",
    homepage = "主頁",
    username = "使用者名稱",
    displayName = "顯示名稱",
    openHomepage = "在瀏覽器中開啟主頁",
    shelfOverview = "書架概覽",
    totalMarks = "累計標記",
    shelfSynced = "已同步目前帳號書架資料",
    recentComplete = "最近完成",
    noCompleteItems = "還沒有完成的條目",
    checkUpdate = "檢查更新",
    checking = "檢查中…",
    currentVersionPrefix = "目前版本 v",
    alreadyLatest = "已是最新版本",
    releasesSubtitle = "在瀏覽器中查看歷史版本",
    appTagline = "NeoDB Lite · 非官方 NeoDB 用戶端",
    logoutTitle = "登出",
    logoutMessage = "將清除本機儲存的登入令牌，需要重新授權才能繼續使用。",
    logout = "登出帳號",
    cancel = "取消",
    notLoggedIn = "未登入",
    newVersionPrefix = "發現新版本 v",
    downloadFailedPrefix = "下載失敗：",
    unknownError = "未知錯誤",
    downloading = "下載中…",
    downloadAndInstall = "下載並安裝",
    goReleases = "前往 Releases",
    sourceLabelPrefix = "來源",
    feedback = "問題回饋",
    autoUpdateCheck = "自動版本更新",
    autoUpdateCheckSubtitle = "軟體啟動時自動檢查新版本",
    preparingDownload = "正在準備下載…",
    downloadingAndVerifying = "正在下載並校驗 APK",
    sourcePrefix = "來源：",
    installerOpened = "系統安裝器已打開。若安裝失敗，可返回此處選擇手動下載。",
    updateFailed = "自動更新失敗：",
    targetVersionPrefix = "目標版本：",
    newVersionReleased = "新版本已經發布。",
    manualDownload = "手動下載",
    later = "稍後",
    retryAutoUpdate = "重試自動更新",
    reopenInstaller = "重新打開安裝器",
    checkUpdateFailed = "自動檢查更新失敗",
    openDownloadPage = "打開下載頁",
    openDownloadPageHint = "可以前往 GitHub Releases 手動檢查並下載最新版本。",
    themeBlueBlack = "藍黑",
    themeTealLight = "海青·淺",
    themeTealDark = "墨綠·深",
    themeSakura = "櫻粉·淺",
    themePurple = "暮紫·深",
    close = "關閉",
    bio = "個人簡介",
    noBio = "暫無簡介"
)

val JA = ZH_HANS.copy(
    navSettings = "設定",
    language = "言語",
    themeColor = "テーマカラー",
    all = "すべて",
    searchScope = "検索範囲を選択",
    searchPlaceholder = "名前を入力して検索",
    search = "検索",
    searchToStart = "検索キーで検索開始",
    noSearchResults = "関連する項目が見つかりません",
    recentSearches = "最近の検索",
    collections = "マイコレクション",
    itemsCount = "件",
    noContent = "コンテンツはありません",
    loading = "読み込み中…",
    loadingMore = "読み込み中…",
    retry = "再試行",
    noRating = "評価なし",
    unnamedItem = "無題の項目",
    networkError = "ネットワークエラー。しばらくしてから再試行してください",
    myRating = "自分の評価",
    loginSubtitle = "NeoDB インスタンスにログインして記録を始める",
    instanceHost = "インスタンスのドメイン",
    login = "ログイン",
    loginHint = "アプリ内で認可ページを開き、認可後に自動でログインします。NeoDB は分散型プラットフォームです。既定のインスタンスは neodb.social ですが、互換インスタンスも指定できます。",
    detail = "詳細",
    back = "戻る",
    peopleCountSuffix = "人",
    mark = "記録",
    editMark = "記録を編集",
    intro = "紹介",
    sources = "ソース",
    externalLink = "外部リンク",
    openInBrowser = "ブラウザで開く",
    status = "ステータス",
    rating = "評価",
    unrated = "未評価",
    visibility = "公開範囲",
    shortCommentOptional = "短いコメント（任意）",
    tagsOptional = "タグ（空白またはカンマ区切り、任意）",
    syncToFediverse = "Fediverse に同期",
    saveMark = "記録を保存",
    deleteMark = "記録を削除",
    saved = "保存しました",
    markDeleted = "記録を削除しました",
    communityContent = "コミュニティ",
    comments = "コメント",
    reviews = "レビュー",
    notes = "ノート",
    openCommunityInBrowser = "Web 版ですべて表示",
    noCommunityContent = "公開コンテンツはありません",
    communityLoadFailed = "コミュニティの読み込みに失敗しました",
    homepage = "ホームページ",
    username = "ユーザー名",
    displayName = "表示名",
    openHomepage = "ホームページをブラウザで開く",
    shelfOverview = "本棚の概要",
    totalMarks = "記録数",
    shelfSynced = "現在のアカウントの本棚データを同期済み",
    recentComplete = "最近完了",
    noCompleteItems = "完了した項目はまだありません",
    checkUpdate = "更新を確認",
    checking = "確認中…",
    currentVersionPrefix = "現在のバージョン v",
    alreadyLatest = "最新バージョンです",
    releasesPage = "Releases ページ",
    releasesSubtitle = "ブラウザでリリース履歴を見る",
    appTagline = "NeoDB Lite · 非公式 NeoDB クライアント",
    logoutTitle = "ログアウト",
    logoutMessage = "保存されたログイントークンを削除します。続けるには再認可が必要です。",
    logout = "ログアウト",
    cancel = "キャンセル",
    notLoggedIn = "未ログイン",
    newVersionPrefix = "新しいバージョン v",
    downloadFailedPrefix = "ダウンロード失敗: ",
    unknownError = "不明なエラー",
    downloading = "ダウンロード中…",
    downloadAndInstall = "ダウンロードしてインストール",
    goReleases = "Releases へ",
    sourceLabelPrefix = "ソース",
    feedback = "問題報告",
    autoUpdateCheck = "自動バージョン更新",
    autoUpdateCheckSubtitle = "アプリ起動時に新しいバージョンを自動確認",
    preparingDownload = "ダウンロードを準備中…",
    downloadingAndVerifying = "APK をダウンロード・検証中",
    sourcePrefix = "ソース：",
    installerOpened = "システムインストーラーが開きました。インストールに失敗した場合は、手動ダウンロードをお試しください。",
    updateFailed = "自動更新に失敗しました：",
    targetVersionPrefix = "対象バージョン：",
    newVersionReleased = "新バージョンがリリースされました。",
    manualDownload = "手動ダウンロード",
    later = "後で",
    retryAutoUpdate = "自動更新を再試行",
    reopenInstaller = "インストーラーを開き直す",
    checkUpdateFailed = "自動更新の確認に失敗しました",
    openDownloadPage = "ダウンロードページを開く",
    openDownloadPageHint = "GitHub Releases で手動確認・ダウンロードできます。",
    themeBlueBlack = "ブルーブラック",
    themeTealLight = "ティール（明）",
    themeTealDark = "ディープグリーン",
    themeSakura = "さくら",
    themePurple = "ミッドナイトパープル",
    close = "閉じる",
    bio = "自己紹介",
    noBio = "自己紹介はありません"
)

val EN = ZH_HANS.copy(
    navSettings = "Settings",
    language = "Language",
    themeColor = "Theme color",
    all = "All",
    searchScope = "Choose search scope",
    searchPlaceholder = "Search by name",
    search = "Search",
    searchToStart = "Press search to begin",
    noSearchResults = "No matching items",
    recentSearches = "Recent searches",
    collections = "My collections",
    itemsCount = "items",
    noContent = "No content yet",
    loading = "Loading…",
    loadingMore = "Loading…",
    retry = "Retry",
    noRating = "No rating",
    unnamedItem = "Untitled item",
    networkError = "Network error. Please try again later.",
    myRating = "My rating",
    loginSubtitle = "Log in to your NeoDB instance and start tracking media",
    instanceHost = "Instance domain",
    login = "Log in",
    loginHint = "The authorization page opens inside the app and signs in after approval. NeoDB is decentralized; neodb.social is the default, but compatible instances also work.",
    detail = "Details",
    back = "Back",
    peopleCountSuffix = " people",
    mark = "Mark",
    editMark = "Edit mark",
    intro = "Overview",
    sources = "Sources",
    externalLink = "External link",
    openInBrowser = "Open in browser",
    status = "Status",
    rating = "Rating",
    unrated = "Unrated",
    visibility = "Visibility",
    shortCommentOptional = "Short comment (optional)",
    tagsOptional = "Tags (space or comma separated, optional)",
    syncToFediverse = "Post to Fediverse",
    saveMark = "Save mark",
    deleteMark = "Delete mark",
    saved = "Saved",
    markDeleted = "Mark deleted",
    communityContent = "Community",
    comments = "Comments",
    reviews = "Reviews",
    notes = "Notes",
    openCommunityInBrowser = "Open web page for all activity",
    noCommunityContent = "No public content yet",
    communityLoadFailed = "Failed to load community content",
    homepage = "Home page",
    username = "Username",
    displayName = "Display name",
    openHomepage = "Open profile in browser",
    shelfOverview = "Shelf overview",
    totalMarks = "Total marks",
    shelfSynced = "Synced from the current account shelf",
    recentComplete = "Recently completed",
    noCompleteItems = "No completed items yet",
    checkUpdate = "Check for updates",
    checking = "Checking…",
    currentVersionPrefix = "Current version v",
    alreadyLatest = "Already up to date",
    releasesPage = "Releases page",
    releasesSubtitle = "View release history in the browser",
    appTagline = "NeoDB Lite · Unofficial NeoDB client",
    logoutTitle = "Log out",
    logoutMessage = "This clears the saved login token. You will need to authorize again before continuing.",
    logout = "Sign out",
    cancel = "Cancel",
    notLoggedIn = "Not logged in",
    newVersionPrefix = "New version v",
    downloadFailedPrefix = "Download failed: ",
    unknownError = "Unknown error",
    downloading = "Downloading…",
    downloadAndInstall = "Download and install",
    goReleases = "Open Releases",
    sourceLabelPrefix = "Source",
    feedback = "Report an issue",
    autoUpdateCheck = "Auto-update",
    autoUpdateCheckSubtitle = "Check for updates on app launch",
    preparingDownload = "Preparing download…",
    downloadingAndVerifying = "Downloading and verifying APK",
    sourcePrefix = "Source: ",
    installerOpened = "System installer opened. If installation fails, use manual download.",
    updateFailed = "Auto-update failed: ",
    targetVersionPrefix = "Target version: ",
    newVersionReleased = "A new version is available.",
    manualDownload = "Manual download",
    later = "Later",
    retryAutoUpdate = "Retry auto-update",
    reopenInstaller = "Reopen installer",
    checkUpdateFailed = "Auto check failed",
    openDownloadPage = "Open download page",
    openDownloadPageHint = "You can check and download the latest version manually on GitHub Releases.",
    themeBlueBlack = "Blue black",
    themeTealLight = "Teal light",
    themeTealDark = "Teal dark",
    themeSakura = "Sakura",
    themePurple = "Midnight purple",
    close = "Close",
    bio = "Bio",
    noBio = "No bio yet"
)

fun appStringsFor(language: AppLanguage): AppStrings = when (AppLanguage.resolve(language)) {
    AppLanguage.ZH_HANS -> ZH_HANS
    AppLanguage.ZH_HANT -> ZH_HANT
    AppLanguage.JA -> JA
    AppLanguage.EN -> EN
    AppLanguage.SYSTEM -> ZH_HANS
}

val LocalAppStrings = staticCompositionLocalOf { ZH_HANS }