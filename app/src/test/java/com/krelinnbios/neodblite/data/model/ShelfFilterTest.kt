package com.krelinnbios.neodblite.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfFilterTest {
    @Test
    fun filterByShelfDropsOtherStatesAndKeepsUnspecifiedOnes() {
        val complete = MarkSchema(shelfType = ShelfType.COMPLETE.apiValue)
        val wishlist = MarkSchema(shelfType = ShelfType.WISHLIST.apiValue)
        val unspecified = MarkSchema(shelfType = null)
        val page = PagedMarks(data = listOf(wishlist, complete, unspecified), pages = 2, count = 3)

        val filtered = page.filterByShelf(ShelfType.COMPLETE)

        assertEquals(listOf(complete, unspecified), filtered.data)
        assertEquals(2, filtered.pages)
        assertEquals(3, filtered.count)
    }
}
