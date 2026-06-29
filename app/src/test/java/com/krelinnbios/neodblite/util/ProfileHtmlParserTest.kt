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

    @Test
    fun profileSectionSkipsDisplayNameAndHandle() {
        val html = """
            <aside class="grid__aside sidebar">
              <section class="profile">
                <article>
                  <details open>
                    <summary>
                      <div>
                        <hgroup>
                          <h6 class="nickname">Display Name</h6>
                          <span class="handler">@demo@example.com</span>
                        </hgroup>
                      </div>
                    </summary>
                    <span class="action"></span>
                    <div>
                      <p>Actual profile text.</p><p>Second line<br>Third line</p>
                    </div>
                  </details>
                </article>
              </section>
            </aside>
        """.trimIndent()

        assertEquals(
            """
            Actual profile text.
            Second line
            Third line
            """.trimIndent(),
            ProfileHtmlParser.parseBio(html)
        )
    }
}
