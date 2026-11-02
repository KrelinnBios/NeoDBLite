package com.krelinnbios.neodblite.ui.vm

import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.PagedTagItems
import com.krelinnbios.neodblite.data.model.TagItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfTagCategoryTest {
    private val tv = TagItem(ItemBrief(uuid = "tv", category = "tv"))
    private val game = TagItem(ItemBrief(uuid = "game", type = "game"))
    private val anotherGame = TagItem(ItemBrief(uuid = "second", category = "game"))

    @Test
    fun countsCategoriesAcrossPagesIncludingMatchesAfterFirstPage() = runBlocking {
        val pages = listOf(
            PagedTagItems(listOf(tv), pages = 2, count = 3),
            PagedTagItems(listOf(game, anotherGame), pages = 2, count = 3)
        )
        assertEquals(mapOf(Category.TV to 1, Category.GAME to 2), countTagCategories { pages[it - 1] })
    }

    @Test
    fun ignoresUnknownAndMissingCategoryForCountsOnly() = runBlocking {
        val members = listOf(TagItem(ItemBrief(category = "unknown")), TagItem(ItemBrief()), TagItem())
        val page = PagedTagItems(members, pages = 1)
        assertEquals(emptyMap<Category, Int>(), countTagCategories { page })
        assertEquals(members, page.membersIn(null))
    }

    @Test
    fun selectedCategoryFiltersMembersInServerOrder() {
        val page = PagedTagItems(listOf(anotherGame, tv, game))
        assertEquals(listOf(anotherGame, game), page.membersIn(Category.GAME))
        assertEquals(emptyList<TagItem>(), page.membersIn(Category.BOOK))
    }
}
