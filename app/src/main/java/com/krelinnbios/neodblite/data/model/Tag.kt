package com.krelinnbios.neodblite.data.model

import com.google.gson.annotations.SerializedName

/** 用户的标签（NeoDB tag）。 */
data class Tag(
    val uuid: String? = null,
    val title: String? = null,
    @SerializedName("item_count") val itemCount: Int? = null,
    val visibility: Int = 0,
    val pinned: Boolean = false
) {
    val bestTitle: String get() = title.orEmpty()
}

data class PagedTags(
    val data: List<Tag> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)

/** 标签下的成员条目。 */
data class TagItem(
    @SerializedName("item") val item: ItemBrief? = null
)

data class PagedTagItems(
    val data: List<TagItem> = emptyList(),
    val pages: Int = 0,
    val count: Int = 0
)
