package com.example.backend.update

import androidx.compose.runtime.Immutable
import com.example.BuildConfig
import com.example.backend.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Immutable
data class JoyTubeReleaseUpdate(
    val version: String,
    val apkUrl: String,
    val releasePageUrl: String
)

/** Reads the public GitHub Releases API; it never requests credentials or account data. */
class GitHubReleaseUpdateChecker(
    private val repository: String = "msxfury/JoyTube",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    fun findNewerApk(): JoyTubeReleaseUpdate? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repository/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "JoyTube-Android/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val payload = response.body?.string() ?: return@use null
            parseLatestReleasePayload(payload, BuildConfig.VERSION_NAME)
        }
    }

    internal fun parseLatestReleasePayload(payload: String, installedVersionName: String): JoyTubeReleaseUpdate? = runCatching {
        val body = JSONObject(payload)
        if (body.optBoolean("draft") || body.optBoolean("prerelease")) return null

        val tag = body.optString("tag_name").trim().ifBlank { body.optString("name").trim() }
        val remoteVersion = comparableVersion(tag) ?: return null
        val installedVersion = comparableVersion(installedVersionName) ?: return null
        if (compareVersions(remoteVersion, installedVersion) <= 0) return null

        val assets = body.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: return null
        val apkUrl = apk.optString("browser_download_url").takeIf { it.startsWith("https://github.com/") }
            ?: return null
        val pageUrl = body.optString("html_url").takeIf { it.startsWith("https://github.com/") } ?: apkUrl
        JoyTubeReleaseUpdate(version = tag.removePrefix("v").removePrefix("V"), apkUrl = apkUrl, releasePageUrl = pageUrl)
    }.getOrNull()

    private fun comparableVersion(raw: String): List<Int>? {
        val match = Regex("(?:^|[^0-9])(\\d+(?:\\.\\d+){1,3})(?:$|[^0-9.])").find(raw) ?: return null
        return match.groupValues[1].split('.').mapNotNull(String::toIntOrNull).takeIf { it.isNotEmpty() }
    }

    private fun compareVersions(left: List<Int>, right: List<Int>): Int {
        val count = maxOf(left.size, right.size)
        for (index in 0 until count) {
            val comparison = (left.getOrNull(index) ?: 0).compareTo(right.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }
}
