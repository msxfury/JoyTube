package com.example.backend.data

import com.example.BuildConfig
import com.example.backend.auth.SupabaseAuthSession
import com.example.model.CategoryItem
import com.example.model.ContentVisibility
import com.example.model.MediaItem
import com.example.model.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Direct, user-JWT Supabase REST repository. RLS remains the ownership boundary. */
class SupabaseJoyTubeRepository {
    private val http = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun saveLocalMediaPointer(context: Context, userId: String, assetId: String, uri: String, mimeType: String?, sizeBytes: Long?) {
        val prefs = context.getSharedPreferences("joytube_local_media", Context.MODE_PRIVATE)
        val key = localMediaKey(userId, assetId)
        prefs.edit().putString(key, JSONObject().put("uri", uri).put("mime", mimeType).put("size", sizeBytes ?: 0L).toString()).apply()
    }

    fun restoreLocalMediaPointers(context: Context, session: SupabaseAuthSession, media: List<MediaItem>): List<MediaItem> {
        val prefs = context.getSharedPreferences("joytube_local_media", Context.MODE_PRIVATE)
        return media.map { item ->
            val raw = prefs.getString(localMediaKey(session.userIdUuid, item.id), null) ?: return@map item
            runCatching {
                val saved = JSONObject(raw)
                val uri = saved.getString("uri")
                context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { }
                item.copy(localContentUri = uri, localMimeType = saved.optString("mime").ifBlank { null }, localSizeBytes = saved.optLong("size").takeIf { it > 0L })
            }.getOrElse { item.copy(localContentUri = null) }
        }
    }

    private fun localMediaKey(userId: String, assetId: String) = "${userId}_$assetId"

    fun loadLibrary(session: SupabaseAuthSession): LibrarySnapshot {
        val owner = session.userIdUuid
        val assetRows = request("GET", "imported_assets?owner_id=eq.$owner&select=*&order=imported_at.desc", session)
        val collections = request("GET", "joytube_collections?owner_id=eq.$owner&select=*&order=created_at.desc", session)
        val itemRows = request("GET", "joytube_collection_items?owner_id=eq.$owner&select=collection_id,imported_asset_id", session)
        val recentRows = request("GET", "recent_imported_content?owner_id=eq.$owner&select=imported_asset_id,imported_at&order=imported_at.desc", session)
        val preferences = request("GET", "user_preferences?user_id=eq.$owner&select=theme,settings", session)
        val settings = preferences.optJSONObject(0)?.optJSONObject("settings") ?: JSONObject()
        val joins = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until itemRows.length()) {
            val row = itemRows.getJSONObject(i)
            joins.getOrPut(row.getString("collection_id")) { mutableListOf() }.add(row.getString("imported_asset_id"))
        }
        val assetsById = (0 until assetRows.length()).map { assetRows.getJSONObject(it) }.associateBy { it.getString("id") }
        val orderedAssets = buildList {
            val seen = mutableSetOf<String>()
            for (i in 0 until recentRows.length()) {
                val id = recentRows.getJSONObject(i).getString("imported_asset_id")
                assetsById[id]?.let { if (seen.add(id)) add(it) }
            }
            for (i in 0 until assetRows.length()) {
                val row = assetRows.getJSONObject(i)
                if (seen.add(row.getString("id"))) add(row)
            }
        }
        val media = buildList {
            for (row in orderedAssets) {
                val id = row.getString("id")
                add(MediaItem(
                    id = id,
                    title = row.getString("title"),
                    importerUsername = session.username,
                    isOwnedByCurrentUser = true,
                    type = if (row.optString("media_kind") == "short") MediaType.Short else MediaType.Video,
                    visibility = ContentVisibility.Public,
                    durationLabel = "00:00",
                    durationSeconds = 0,
                    categoryTag = "All",
                    artwork = com.example.ui.components.ArtworkPreset.SynthwaveStudio,
                    isFavourite = row.optBoolean("is_favourite"),
                    isBookmarked = row.optBoolean("is_favourite")
                ))
            }
        }
        val cats = buildList {
            for (i in 0 until collections.length()) {
                val row = collections.getJSONObject(i)
                add(CategoryItem(
                    id = row.getString("id"), name = row.getString("name"),
                    creatorUsername = session.username, isOwnedByCurrentUser = true,
                    visibility = if (row.optString("visibility") == "public") ContentVisibility.Public else ContentVisibility.Private,
                    artwork = com.example.ui.components.ArtworkPreset.SynthwaveStudio,
                    sourceDomain = "joytube.app", mediaIds = joins[row.getString("id")].orEmpty(),
                    description = row.optString("description"),
                    isFavourite = row.optBoolean("is_favourite"), isBookmarked = row.optBoolean("is_favourite")
                ))
            }
        }
        return LibrarySnapshot(media, cats, preferences.optJSONObject(0)?.optString("theme") ?: "glass", settings)
    }

    fun createCollection(session: SupabaseAuthSession, category: CategoryItem): CategoryItem {
        val body = JSONObject().put("owner_id", session.userIdUuid).put("name", category.name)
            .put("description", category.description).put("visibility", visibility(category.visibility))
            .put("is_favourite", category.isFavourite)
        val rows = request("POST", "joytube_collections?select=id", session, body, "return=representation")
        val id = rows.optJSONObject(0)?.optString("id")
            ?: throw IllegalStateException("Supabase did not return the collection id")
        // Map's placeholder ID is replaced by caller using this UUID return value.
        return category.copy(id = id)
    }

    fun updateCollection(session: SupabaseAuthSession, category: CategoryItem) {
        request("PATCH", "joytube_collections?id=eq.${enc(category.id)}&owner_id=eq.${session.userIdUuid}", session,
            JSONObject().put("name", category.name).put("description", category.description)
                .put("visibility", visibility(category.visibility)).put("is_favourite", category.isFavourite), "return=minimal")
    }

    fun deleteCollection(session: SupabaseAuthSession, id: String) {
        request("DELETE", "joytube_collections?id=eq.${enc(id)}&owner_id=eq.${session.userIdUuid}", session, prefer = "return=minimal")
    }

    fun setCollectionFavourite(session: SupabaseAuthSession, id: String, favourite: Boolean) {
        request("PATCH", "joytube_collections?id=eq.${enc(id)}&owner_id=eq.${session.userIdUuid}", session,
            JSONObject().put("is_favourite", favourite), "return=minimal")
    }

    fun removeAssetFromCollection(session: SupabaseAuthSession, collectionId: String, assetId: String) {
        request("DELETE", "joytube_collection_items?collection_id=eq.${enc(collectionId)}&imported_asset_id=eq.${enc(assetId)}&owner_id=eq.${session.userIdUuid}", session, prefer = "return=minimal")
    }

    fun setFavourite(session: SupabaseAuthSession, mediaId: String, isFavourite: Boolean) {
        request("PATCH", "imported_assets?id=eq.${enc(mediaId)}&owner_id=eq.${session.userIdUuid}", session,
            JSONObject().put("is_favourite", isFavourite), "return=minimal")
    }

    fun createImportedAsset(session: SupabaseAuthSession, item: MediaItem): String {
        val kind = if (item.type == MediaType.Short) "short" else "video"
        val rows = request("POST", "imported_assets?select=id", session,
            JSONObject().put("owner_id", session.userIdUuid).put("title", item.title).put("media_kind", kind)
                .put("is_favourite", item.isFavourite), "return=representation")
        return rows.optJSONObject(0)?.optString("id") ?: throw IllegalStateException("Supabase did not return imported item id")
    }

    fun recordDownload(session: SupabaseAuthSession, assetId: String) {
        request("POST", "rpc/record_imported_asset_download", session, JSONObject().put("p_asset_id", assetId))
    }

    fun recordView(session: SupabaseAuthSession, assetId: String) {
        request("POST", "rpc/record_imported_asset_view", session, JSONObject().put("p_asset_id", assetId))
    }

    fun saveTheme(session: SupabaseAuthSession, theme: String) {
        request("POST", "user_preferences?on_conflict=user_id", session,
            JSONObject().put("user_id", session.userIdUuid).put("theme", if (theme == "glass") "glass" else "normal"),
            "resolution=merge-duplicates,return=minimal")
    }

    fun saveSettings(session: SupabaseAuthSession, settings: JSONObject) {
        request("POST", "user_preferences?on_conflict=user_id", session,
            JSONObject().put("user_id", session.userIdUuid).put("settings", settings),
            "resolution=merge-duplicates,return=minimal")
    }

    fun saveProfile(session: SupabaseAuthSession, displayName: String) {
        request("PATCH", "profiles?id=eq.${session.userIdUuid}", session,
            JSONObject().put("display_name", displayName.trim().take(80).ifBlank { "JoyTube User" }), "return=minimal")
    }

    fun attachAsset(session: SupabaseAuthSession, collectionId: String, assetId: String, position: Int) {
        request("POST", "joytube_collection_items", session,
            JSONObject().put("collection_id", collectionId).put("owner_id", session.userIdUuid)
                .put("imported_asset_id", assetId).put("position_index", position), "resolution=ignore-duplicates,return=minimal")
    }

    fun recordRecentImport(session: SupabaseAuthSession, assetId: String) {
        request("POST", "recent_imported_content?on_conflict=owner_id,imported_asset_id", session,
            JSONObject().put("owner_id", session.userIdUuid).put("imported_asset_id", assetId),
            "resolution=merge-duplicates,return=minimal")
    }

    private fun request(method: String, path: String, session: SupabaseAuthSession, body: JSONObject? = null, prefer: String? = null): JSONArray {
        val builder = Request.Builder().url("${BuildConfig.SUPABASE_URL}/rest/v1/$path")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer ${session.supabaseAccessTokenJwt}")
            .header("Accept", "application/json")
        if (body != null) builder.header("Content-Type", "application/json")
        if (prefer != null) builder.header("Prefer", prefer)
        val requestBody = body?.toString()?.toRequestBody(jsonType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: "".toRequestBody(jsonType))
            "PATCH" -> builder.patch(requestBody ?: "{}".toRequestBody(jsonType))
            "DELETE" -> builder.delete()
            else -> throw IllegalArgumentException("Unsupported Supabase method: $method")
        }
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("JoyTube cloud sync failed (${response.code}): ${text.take(240)}")
            if (text.isBlank()) return JSONArray()
            return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONArray().put(JSONObject(text))
        }
    }

    private fun visibility(value: ContentVisibility) = if (value == ContentVisibility.Public) "public" else "private"
    private fun enc(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

data class LibrarySnapshot(val media: List<MediaItem>, val collections: List<CategoryItem>, val theme: String, val settings: JSONObject)
