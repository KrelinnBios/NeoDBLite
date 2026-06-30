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
    val rating: Double? = null
)