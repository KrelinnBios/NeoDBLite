package com.krelinnbios.neodblite.data.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * NeoDB 条目（catalog item）。各类目（书/影/剧/乐/游/播客/演出）字段不尽相同，
 * 这里以宽松可空的方式收纳公共字段与部分类目特有字段，未命中的字段忽略即可。
 */
data class ItemBrief(
    val id: String? = null,
    val type: String? = null,
    val uuid: String? = null,
    val url: String? = null,
    @SerializedName("api_url") val apiUrl: String? = null,
    val category: String? = null,
    @SerializedName("parent_uuid") val parentUuid: String? = null,
    @SerializedName("display_title") val displayTitle: String? = null,
    val title: String? = null,
    val brief: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    val rating: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int? = null,
    val tags: List<String>? = null,
    @SerializedName("external_resources") val externalResources: List<ExternalResource>? = null,

    // 类目特有的常见字段（可空）。部分实例会把人员字段返回成对象数组，
    // 统一用 FlexibleStringListDeserializer 兼容「字符串/对象」两种形态。
    @SerializedName("author") @JsonAdapter(FlexibleStringListDeserializer::class) val author: List<String>? = null,
    @SerializedName("pub_house") val pubHouse: String? = null,
    @SerializedName("pub_year") val pubYear: Int? = null,
    val year: Int? = null,
    @JsonAdapter(FlexibleStringListDeserializer::class) val director: List<String>? = null,
    @JsonAdapter(FlexibleStringListDeserializer::class) val actor: List<String>? = null,
    @JsonAdapter(FlexibleStringListDeserializer::class) val genre: List<String>? = null,
    @JsonAdapter(FlexibleStringListDeserializer::class) val artist: List<String>? = null,
) {
    /** 优先展示标题：display_title 优先，回退 title。 */
    val bestTitle: String
        get() = displayTitle?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: "未命名条目"

    /** 用于详情页拉取的相对/绝对地址；优先 api_url，回退由 url 推导。 */
    val fetchPath: String?
        get() = apiUrl?.takeIf { it.isNotBlank() } ?: url?.takeIf { it.isNotBlank() }
}

/** 条目在第三方站点的来源链接（豆瓣、IMDb、Bangumi 等）。 */
data class ExternalResource(
    val url: String? = null
)

data class SearchResult(
    val data: List<ItemBrief> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)

/** 书架上的一条标记，含其关联条目。 */
data class MarkSchema(
    @SerializedName("shelf_type") val shelfType: String? = null,
    val visibility: Int = 0,
    @SerializedName("item") val item: ItemBrief? = null,
    @SerializedName("created_time") val createdTime: String? = null,
    @SerializedName("comment_text") val commentText: String? = null,
    @SerializedName("rating_grade") val ratingGrade: Int? = null,
    val tags: List<String> = emptyList()
)

data class PagedMarks(
    val data: List<MarkSchema> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)

/** 创建/更新标记的请求体。 */
data class MarkInRequest(
    @SerializedName("shelf_type") val shelfType: String,
    val visibility: Int,
    @SerializedName("comment_text") val commentText: String? = null,
    @SerializedName("rating_grade") val ratingGrade: Int? = null,
    val tags: List<String> = emptyList(),
    @SerializedName("created_time") val createdTime: String? = null,
    @SerializedName("post_to_fediverse") val postToFediverse: Boolean = false
)

data class NeoUser(
    val url: String? = null,
    @SerializedName("external_acct") val externalAcct: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    // 简介：不同实例字段名可能不同，尽量都收一下。
    val note: String? = null,
    val summary: String? = null,
    val bio: String? = null
) {
    val bestName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: externalAcct?.takeIf { it.isNotBlank() }
            ?: "我"

    /** 个人简介纯文本：取 note/summary/bio 中第一个非空，去掉 HTML 标签。 */
    val bioText: String?
        get() {
            val raw = listOf(note, summary, bio).firstOrNull { !it.isNullOrBlank() } ?: return null
            return raw.replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace(Regex("\\s+"), " ")
                .trim()
                .takeIf { it.isNotBlank() }
        }
}

/** POST /api/v1/apps 的返回。 */
data class AppRegistration(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String,
    val name: String? = null,
    @SerializedName("redirect_uri") val redirectUri: String? = null
)

/** POST /oauth/token 的返回。 */
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String? = null,
    val scope: String? = null
)
