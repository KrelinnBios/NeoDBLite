package com.krelinnbios.neodblite.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {
    @Test
    fun parsesGitHubReleaseAndSelectsApkAsset() {
        val info = AppUpdateManager.parseReleaseJson(
            """
            {
              "tag_name": "v1.2.3",
              "body": "Release notes",
              "html_url": "https://github.com/KrelinnBios/NeoDBLite/releases/tag/v1.2.3",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "checksums.txt",
                  "content_type": "text/plain",
                  "browser_download_url": "https://example.com/checksums.txt"
                },
                {
                  "name": "NeoDB Lite.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
            """.trimIndent()
        )

        requireNotNull(info)
        assertEquals("1.2.3", info.versionName)
        assertEquals("Release notes", info.releaseNotes)
        assertEquals("https://example.com/app.apk", info.apkUrl)
        assertEquals(
            "https://github.com/KrelinnBios/NeoDBLite/releases/tag/v1.2.3",
            info.releasePageUrl
        )
    }

    @Test
    fun selectsLatestFromReleaseArray() {
        val info = AppUpdateManager.parseReleaseJson(
            """
            [
              {"tag_name":"v1.0.0","draft":false,"prerelease":false,
               "assets":[{"name":"a.apk","browser_download_url":"https://example.com/old.apk"}]},
              {"tag_name":"v1.3.0","draft":false,"prerelease":false,
               "assets":[{"name":"b.apk","browser_download_url":"https://example.com/new.apk"}]}
            ]
            """.trimIndent()
        )
        requireNotNull(info)
        assertEquals("1.3.0", info.versionName)
        assertEquals("https://example.com/new.apk", info.apkUrl)
    }

    @Test
    fun rejectsReleaseWithoutApkAndPrerelease() {
        assertNull(
            AppUpdateManager.parseReleaseJson(
                """{"tag_name":"v2.0.0","draft":false,"prerelease":false,"assets":[]}"""
            )
        )
        assertNull(
            AppUpdateManager.parseReleaseJson(
                """
                {
                  "tag_name":"v2.0.0-beta",
                  "draft":false,
                  "prerelease":true,
                  "assets":[{
                    "name":"app.apk",
                    "browser_download_url":"https://example.com/app.apk"
                  }]
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun buildsGithubFirstThenMirrorCandidates() {
        val apkUrl =
            "https://github.com/KrelinnBios/NeoDBLite/releases/download/v1.0.1/NeoDB.Lite.apk"
        val candidates = AppUpdateManager.buildDownloadCandidates(apkUrl)

        assertEquals(apkUrl, candidates.first())
        assertEquals(AppUpdateManager.DOWNLOAD_MIRROR_PREFIXES.size + 1, candidates.size)
        AppUpdateManager.DOWNLOAD_MIRROR_PREFIXES.forEach { prefix ->
            assertTrue(candidates.contains(prefix + apkUrl))
        }
    }

    @Test
    fun nonGithubUrlHasNoMirrorFallback() {
        val apkUrl = "https://example.com/app.apk"
        assertEquals(listOf(apkUrl), AppUpdateManager.buildDownloadCandidates(apkUrl))
    }

    @Test
    fun comparesSemanticVersions() {
        assertTrue(AppUpdateManager.compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdateManager.compareVersions("2.0.0", "1.99.99") > 0)
        assertEquals(0, AppUpdateManager.compareVersions("v1.0.0", "1.0"))
    }
}
