package com.krelinnbios.neodblite.data.model

/** 书架状态。NeoDB 的 shelf_type 取值固定，但中文标签随类目变化（想读/想看…）。 */
enum class ShelfType(val apiValue: String) {
    WISHLIST("wishlist"),
    PROGRESS("progress"),
    COMPLETE("complete"),
    DROPPED("dropped");

    companion object {
        fun fromApi(value: String?): ShelfType? =
            entries.firstOrNull { it.apiValue == value }
    }
}

/** 可见性：0 公开 / 1 仅关注者 / 2 仅自己。 */
enum class Visibility(val apiValue: Int, val label: String) {
    PUBLIC(0, "公开"),
    FOLLOWERS(1, "仅关注者"),
    PRIVATE(2, "仅自己");

    companion object {
        fun fromApi(value: Int): Visibility =
            entries.firstOrNull { it.apiValue == value } ?: PUBLIC
    }
}

/** 类目。apiValue 用于搜索/趋势接口；trendingPath 为趋势接口路径段（music 合并 album）。 */
enum class Category(
    val apiValue: String,
    val label: String,
    val trendingPath: String?
) {
    BOOK("book", "图书", "book"),
    MOVIE("movie", "电影", "movie"),
    TV("tv", "剧集", "tv"),
    MUSIC("music", "音乐", "music"),
    GAME("game", "游戏", "game"),
    PODCAST("podcast", "播客", "podcast"),
    PERFORMANCE("performance", "演出", "performance");

    companion object {
        fun fromApi(value: String?): Category? =
            entries.firstOrNull { it.apiValue == value || value == it.trendingPath }
    }
}

/**
 * 书架状态在不同类目下的中文动词。
 * 例如 BOOK → 想读/在读/读过；MOVIE/TV → 想看/在看/看过。
 */
fun shelfLabel(shelf: ShelfType, category: Category?): String {
    val verbs = when (category) {
        Category.BOOK -> Triple("想读", "在读", "读过")
        Category.MOVIE, Category.TV -> Triple("想看", "在看", "看过")
        Category.MUSIC -> Triple("想听", "在听", "听过")
        Category.PODCAST -> Triple("想听", "在听", "听过")
        Category.GAME -> Triple("想玩", "在玩", "玩过")
        Category.PERFORMANCE -> Triple("想看", "在看", "看过")
        null -> Triple("想要", "进行中", "已完成")
    }
    return when (shelf) {
        ShelfType.WISHLIST -> verbs.first
        ShelfType.PROGRESS -> verbs.second
        ShelfType.COMPLETE -> verbs.third
        ShelfType.DROPPED -> "搁置"
    }
}
