package com.krelinnbios.neodblite.data

import android.content.Context

/** 最近搜索关键词的本地存储（SharedPreferences，去重、限量、最近优先）。 */
class SearchHistoryStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY_QUERIES, "").orEmpty()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** 把关键词置顶加入历史，返回更新后的列表。 */
    fun add(query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return load()
        val list = (listOf(q) + load())
            .distinctBy { it.lowercase() }
            .take(MAX)
        prefs.edit().putString(KEY_QUERIES, list.joinToString("\n")).apply()
        return list
    }

    fun remove(query: String): List<String> {
        val list = load().filterNot { it.equals(query, ignoreCase = true) }
        prefs.edit().putString(KEY_QUERIES, list.joinToString("\n")).apply()
        return list
    }

    fun clear() {
        prefs.edit().remove(KEY_QUERIES).apply()
    }

    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_QUERIES = "queries"
        private const val MAX = 12
    }
}
