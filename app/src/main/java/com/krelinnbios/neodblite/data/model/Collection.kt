package com.krelinnbios.neodblite.data.model

import com.google.gson.annotations.SerializedName

/** NeoDB 合集（collection）。 */
data class Collection(
    val uuid: String? = null,
    val url: String? = null,
    val title: String? = null,
    val brief: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("item_count") val itemCount: Int? = null,
    @SerializedName("item_count_by_category") val itemCountByCategory: Map<String, Int> = emptyMap(),
    val visibility: Int = 0,
    @SerializedName("created_time") val createdTime: String? = null
) {
    val bestTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "未命名合集"

    val totalItemCount: Int?
        get() = itemCount ?: itemCountByCategory.values.sum().takeIf { itemCountByCategory.isNotEmpty() }
}

data class PagedCollections(
    val data: List<Collection> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)

/** 合集内的一个成员条目。 */
data class CollectionItem(
    @SerializedName("item") val item: ItemBrief? = null,
    val note: String? = null,
    val notes: String? = null,
    val position: Int? = null
) {
    val memberNote: String? get() = note?.takeIf { it.isNotBlank() } ?: notes?.takeIf { it.isNotBlank() }
}

data class PagedCollectionItems(
    val data: List<CollectionItem> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)
