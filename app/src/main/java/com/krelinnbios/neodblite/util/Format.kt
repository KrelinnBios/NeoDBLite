package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief

object Format {

    /** 评分文本：NeoDB 评分为 0~10 一位小数；无评分显示占位。 */
    fun ratingText(rating: Double?): String =
        if (rating == null || rating <= 0.0) "暂无评分" else String.format("%.1f", rating)

    /** 条目副标题：聚合作者/导演/年份/出版社等可用信息。 */
    fun subtitle(item: ItemBrief): String {
        val parts = mutableListOf<String>()
        item.author?.firstOrNull()?.let { parts.add(it) }
        item.director?.firstOrNull()?.let { parts.add(it) }
        item.artist?.firstOrNull()?.let { parts.add(it) }
        (item.pubYear ?: item.year)?.let { parts.add("$it") }
        item.pubHouse?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString(" · ")
    }

    /** 类目中文名。 */
    fun categoryLabel(item: ItemBrief): String =
        Category.fromApi(item.category ?: item.type)?.label ?: (item.category ?: "")
}
