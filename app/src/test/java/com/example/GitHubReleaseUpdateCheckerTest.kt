package com.example

import com.example.backend.update.GitHubReleaseUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class GitHubReleaseUpdateCheckerTest {
    private val checker = GitHubReleaseUpdateChecker()

    @Test
    fun detectsGreaterStableReleaseWithApkAsset() {
        val update = checker.parseLatestReleasePayload(releaseJson(tag = "v1.0.3"), "1.0.2")
        assertNotNull(update)
        assertEquals("1.0.3", update?.version)
        assertEquals("https://github.com/msxfury/JoyTube/releases/download/v1.0.3/JoyTube.apk", update?.apkUrl)
    }

    @Test
    fun ignoresSameOrOlderVersion() {
        assertNull(checker.parseLatestReleasePayload(releaseJson(tag = "v1.0.2"), "1.0.2"))
        assertNull(checker.parseLatestReleasePayload(releaseJson(tag = "v1.0.1"), "1.0.2"))
    }

    @Test
    fun ignoresDraftPrereleaseAndReleaseWithoutApk() {
        assertNull(checker.parseLatestReleasePayload(releaseJson(tag = "v1.1.0", draft = true), "1.0.2"))
        assertNull(checker.parseLatestReleasePayload(releaseJson(tag = "v1.1.0", prerelease = true), "1.0.2"))
        assertNull(checker.parseLatestReleasePayload(releaseJson(tag = "v1.1.0", apk = false), "1.0.2"))
    }

    @Test
    fun comparesVersionComponentsNumerically() {
        assertNotNull(checker.parseLatestReleasePayload(releaseJson(tag = "JoyTube 1.0.10"), "1.0.9"))
    }

    private fun releaseJson(
        tag: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        apk: Boolean = true
    ) = """
        {
          "tag_name":"$tag",
          "draft":$draft,
          "prerelease":$prerelease,
          "html_url":"https://github.com/msxfury/JoyTube/releases/tag/$tag",
          "assets":${if (apk) "[{\"name\":\"JoyTube.apk\",\"browser_download_url\":\"https://github.com/msxfury/JoyTube/releases/download/$tag/JoyTube.apk\"}]" else "[]"}
        }
    """.trimIndent()
}
