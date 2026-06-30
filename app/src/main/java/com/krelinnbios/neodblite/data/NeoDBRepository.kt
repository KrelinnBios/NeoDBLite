package com.krelinnbios.neodblite.data

import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.Collection
import com.krelinnbios.neodblite.data.model.CommunityEntry
import com.krelinnbios.neodblite.data.model.CommunityEntryType
import com.krelinnbios.neodblite.data.model.PagedCollectionItems
import com.krelinnbios.neodblite.data.model.PagedCollections
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.data.model.PagedMarks
import com.krelinnbios.neodblite.data.model.SearchResult
import com.krelinnbios.neodblite.data.model.ShelfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.krelinnbios.neodblite.util.CommunityHtmlParser
import com.krelinnbios.neodblite.util.ProfileHtmlParser
import java.io.IOException

/** 业务数据仓库：包装 [NeoDBApi]，统一在 IO 线程执行并返回 Result。 */
class NeoDBRepository(private val client: NeoDBClient) {

    private val api get() = client.api

    suspend fun me(): Result<NeoUser> = io { api.me() }

    suspend fun profileBio(user: NeoUser, host: String): Result<String?> = io {
        val path = userProfilePath(user, host)
        if (path.isNullOrBlank()) null else ProfileHtmlParser.parseBio(api.htmlPage(path).string())
    }

    suspend fun search(query: String, category: Category?, page: Int): Result<SearchResult> =
        io { api.search(query, category?.apiValue, page) }

    suspend fun trending(category: Category): Result<List<ItemBrief>> = io {
        api.trending(category.trendingPath ?: category.apiValue)
    }

    suspend fun myCollections(page: Int): Result<PagedCollections> = io { api.myCollections(page) }

    suspend fun collection(uuid: String): Result<Collection> = io { api.collection(uuid) }

    suspend fun collectionItems(uuid: String, page: Int): Result<PagedCollectionItems> =
        io { api.collectionItems(uuid, page) }

    /** 用条目的 api_url/url（相对 baseUrl）拉详情。 */
    suspend fun item(path: String): Result<ItemBrief> = io {
        api.itemByPath(path.removePrefix("/"))
    }
    suspend fun itemCommunity(item: ItemBrief, host: String): Result<List<CommunityEntry>> = io {
        val path = item.url?.takeIf { it.isNotBlank() }
            ?: item.id?.takeIf { it.startsWith("http", ignoreCase = true) }
                ?.substringAfter(host, missingDelimiterValue = "")
        val normalized = path?.removePrefix("/")?.substringBefore('?')?.trimEnd('/').orEmpty()
        if (normalized.isBlank()) {
            emptyList()
        } else {
            listOf(
                CommunityEntryType.COMMENT to "comments",
                CommunityEntryType.REVIEW to "reviews",
                CommunityEntryType.NOTE to "notes"
            ).flatMap { (type, suffix) ->
                runCatching {
                    val html = api.htmlFragment("$normalized/$suffix").string()
                    CommunityHtmlParser.parse(type, html, host)
                }.getOrElse { emptyList() }
            }
        }
    }

    suspend fun shelf(type: ShelfType, category: Category?, page: Int): Result<PagedMarks> =
        io { api.shelf(type.apiValue, category?.apiValue, page) }

    /** 查询某条目的当前标记；未标记（404）返回 null。 */
    suspend fun mark(uuid: String): Result<MarkSchema?> = io {
        val resp = api.getMark(uuid)
        when {
            resp.isSuccessful -> resp.body()
            resp.code() == 404 -> null
            else -> throw IOException("读取标记失败：HTTP ${resp.code()}")
        }
    }

    suspend fun postMark(uuid: String, body: MarkInRequest): Result<Unit> = io {
        val resp = api.postMark(uuid, body)
        if (!resp.isSuccessful) throw IOException("保存标记失败：HTTP ${resp.code()}")
        Unit
    }

    suspend fun deleteMark(uuid: String): Result<Unit> = io {
        val resp = api.deleteMark(uuid)
        if (!resp.isSuccessful && resp.code() != 404) {
            throw IOException("删除标记失败：HTTP ${resp.code()}")
        }
        Unit
    }

    private fun userProfilePath(user: NeoUser, host: String): String? {
        val normalizedHost = host
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        user.url?.trim()?.takeIf { it.isNotBlank() }?.let { rawUrl ->
            if (rawUrl.startsWith("http", ignoreCase = true)) return rawUrl
            val withoutHost = if (normalizedHost.isNotBlank()) {
                rawUrl.removePrefix(normalizedHost).removePrefix("/")
            } else rawUrl
            return withoutHost.removePrefix("/")
        }
        val username = user.username?.trim()?.removePrefix("@").takeIf { !it.isNullOrBlank() }
        if (!username.isNullOrBlank()) return "users/$username"
        val acct = user.externalAcct
            ?.trim()
            ?.removePrefix("@")
            ?.substringBefore("@")
            .takeIf { !it.isNullOrBlank() }
        return acct?.let { "users/$it" }
    }

    private suspend inline fun <T> io(crossinline block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) { runCatching { block() } }
}
