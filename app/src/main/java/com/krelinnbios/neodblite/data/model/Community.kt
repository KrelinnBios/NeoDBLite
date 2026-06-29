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
    val url: String?
)