package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.Category
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagHtmlParserTest {
    private val page = "https://example.org/users/bios/tags/?category=game".toHttpUrl()
    private val gamePage = """
        <div class="tag-list">
          <span class="tag-list-entry">
            <span><a href="/users/bios/tags/%E6%81%8B%E7%88%B1/?category=game">
              <i class="fa-solid fa-crown"></i> 恋爱
            </a></span>
            <span>(41)</span>
          </span>
        </div>
        <select name="category"><option value="" >全部</option><option value="game" selected>游戏</option></select>
    """.trimIndent()

    @Test fun readsCategoryCountWithoutFetchingTagMembers() {
        assertEquals(mapOf("恋爱" to 41), TagHtmlParser.counts(gamePage, page, Category.GAME))
    }

    @Test fun readsUnfilteredCountFromSavedPageMarkup() {
        val unfiltered = gamePage.replace("?category=game", "").replace("value=\"game\" selected", "value=\"\" selected=\"\"")
        assertEquals(mapOf("恋爱" to 41), TagHtmlParser.counts(unfiltered, page, null))
    }

    @Test fun distinguishesEmptyCategoryFromUnsupportedOrWrongPage() {
        val empty = gamePage.replaceRange(gamePage.indexOf("<span"), gamePage.indexOf("</div>"), "暂无标签")
        assertEquals(emptyMap<String, Int>(), TagHtmlParser.counts(empty, page, Category.GAME))
        assertNull(TagHtmlParser.counts(gamePage, page, Category.BOOK))
        assertNull(TagHtmlParser.counts(gamePage.replace("(41)", "未知"), page, Category.GAME))
        assertNull(TagHtmlParser.counts("<form>Login</form>", page, Category.GAME))
    }
}
