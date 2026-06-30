package com.krelinnbios.neodblite.data.model

enum class CommunityEntryType {
    COMMENT,
    REVIEW,
    NOTE
}

data class CommunityEntry(
    val type: CommunityEntryType,
    val author: String,
    val action: String,
    val content: String,
    val url: String?,
    /** 评论者评分，0~10；无评分为 null。 */
    val rating: Double? = null,
    /** 发布时间（NeoDB 站点显示的相对时间，如 "1yr"）；解析不到为 null。 */
    val date: String? = null
)