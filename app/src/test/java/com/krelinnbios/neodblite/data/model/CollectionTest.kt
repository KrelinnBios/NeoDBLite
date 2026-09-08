package com.krelinnbios.neodblite.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionTest {
    @Test
    fun keepsLegacyItemCountWhenPresent() {
        val collection = Collection(itemCount = 3, itemCountByCategory = mapOf("movie" to 5))

        assertEquals(3, collection.totalItemCount)
    }

    @Test
    fun derivesItemCountFromCategoryCounts() {
        val collection = Collection(itemCountByCategory = mapOf("movie" to 5, "tv" to 2))

        assertEquals(7, collection.totalItemCount)
    }

    @Test
    fun parsesCurrentApiCategoryCountField() {
        val collection = Gson().fromJson(
            "{\"item_count_by_category\":{\"movie\":320,\"tv\":1}}",
            Collection::class.java
        )

        assertEquals(321, collection.totalItemCount)
    }

    @Test
    fun leavesCountUnknownWhenNoCountIsReturned() {
        assertNull(Collection().totalItemCount)
    }
}
