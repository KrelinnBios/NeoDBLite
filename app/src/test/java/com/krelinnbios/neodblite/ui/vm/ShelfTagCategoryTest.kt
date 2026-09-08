package com.krelinnbios.neodblite.ui.vm

import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.TagItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfTagCategoryTest {
    @Test
    fun derivesCategoriesFromItemCategory() {
        val items = listOf(
            TagItem(ItemBrief(uuid = "1", category = "movie")),
            TagItem(ItemBrief(uuid = "2", category = "game")),
            TagItem(ItemBrief(uuid = "3", category = "movie"))
        )
        assertEquals(setOf(Category.MOVIE, Category.GAME), tagCategoriesOf(items))
    }

    @Test
    fun ignoresItemsWithoutCategoryOrType() {
        val items = listOf(
            TagItem(ItemBrief(uuid = "1", category = "book")),
            TagItem(ItemBrief(uuid = "2"))
        )
        assertEquals(setOf(Category.BOOK), tagCategoriesOf(items))
    }

    @Test
    fun fallsBackToTypeWhenCategoryMissing() {
        val items = listOf(TagItem(ItemBrief(uuid = "1", type = "podcast")))
        assertEquals(setOf(Category.PODCAST), tagCategoriesOf(items))
    }

    @Test
    fun returnsEmptySetForUnknownCategory() {
        val items = listOf(TagItem(ItemBrief(uuid = "1", category = "unknown")))
        assertTrue(tagCategoriesOf(items).isEmpty())
    }
}