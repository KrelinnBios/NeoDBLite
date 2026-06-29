package com.krelinnbios.neodblite.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHtmlParserTest {
    @Test
    fun parsesParagraphBioFromProfileDiv() {
        val html = """
            <main>
              <div class="layout"><p>Ignore this short block</p></div>
              <div>
                <p>Profile intro:</p><p>Line one<br>Line two<br>Line three</p><p>Final note</p>
                <br>
              </div>
            </main>
        """.trimIndent()

        assertEquals(
            """
            Profile intro:
            Line one
            Line two
            Line three
            Final note
            """.trimIndent(),
            ProfileHtmlParser.parseBio(html)
        )
    }
}
