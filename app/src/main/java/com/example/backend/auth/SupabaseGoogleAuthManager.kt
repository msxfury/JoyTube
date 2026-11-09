package com.example.backend.auth

import android.app.Activity
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Immutable
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A JoyTube session. Google ID tokens are exchanged and discarded; only the Supabase
 * session is retained, encrypted with a key held by Android Keystore. */
@Immutable
data class SupabaseAuthSession(
    val userIdUuid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val supabaseAccessTokenJwt: String,
    val supabaseRefreshToken: String,
    val expiresAtEpochMs: Long,
    val isGoogleIdentityLinked: Boolean = true,
    // Google identity sign-in does not authorize YouTube account APIs or sign the embedded player in.
    val isYouTubeReadonlyConnected: Boolean = false
)

class SupabaseGoogleAuthManager {
    private val http = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private var appContext: Context? = null
    private var activeSession: SupabaseAuthSession? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        activeSession = readStoredSession()
    }

    fun currentSession(): SupabaseAuthSession? = activeSession

    suspend fun signInWithGoogle(activity: Activity): SupabaseAuthSession = withContext(Dispatchers.IO) {
        val context = appContext ?: activity.applicationContext.also { appContext = it }
        require(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Google sign-in is not configured in this build." }

        val nonce = randomNonce()
        // This is an explicit Sign in with Google button. Use the dedicated Credential
        // Manager option so users can sign in even when no previously authorized
        // credential is available on the device.
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(sha256Hex(nonce))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        // Credential Manager must be invoked from the UI thread and with the foreground Activity.
        val credential = withContext(Dispatchers.Main) {
            CredentialManager.create(context).getCredential(activity, request).credential
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google did not return a supported sign-in credential.")
        }

        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val tokenResponse = postAuth(
            path = "token?grant_type=id_token",
            body = JSONObject()
                .put("provider", "google")
                .put("id_token", googleIdToken)
                .put("nonce", nonce)
        )
        val user = tokenResponse.optJSONObject("user")
            ?: throw IllegalStateException("Supabase authenticated Google but returned no user profile.")
        val userMetadata = user.optJSONObject("user_metadata") ?: JSONObject()
        val email = user.optString("email").trim()
        val userId = user.optString("id").trim()
        if (userId.isBlank() || email.isBlank()) {
            throw IllegalStateException("Google sign-in did not provide the email required for a JoyTube profile.")
        }

        val displayName = firstNonBlank(
            userMetadata.optString("full_name"),
            userMetadata.optString("name"),
            email.substringBefore('@')
        )
        val avatarUrl = firstNonBlankOrNull(
            userMetadata.optString("avatar_url"),
            userMetadata.optString("picture")
        )
        val session = SupabaseAuthSession(
            userIdUuid = userId,
            email = email,
            username = usernameFor(email, userId),
            displayName = displayName,
            avatarUrl = avatarUrl,
            supabaseAccessTokenJwt = tokenResponse.getString("access_token"),
            supabaseRefreshToken = tokenResponse.getString("refresh_token"),
            expiresAtEpochMs = tokenResponse.optLong("expires_at")
                .takeIf { it > 0L }?.times(1000L)
                ?: (System.currentTimeMillis() + tokenResponse.optLong("expires_in", 3600L) * 1000L)
        )

        // Email is held by Supabase Auth. User profile/preferences are stored in the RLS-protected
        // JoyTube tables; no Google password, Google access token, YouTube data, or media file is stored.
        ensureProfileAndPreferences(session)
        activeSession = session
        writeStoredSession(session)
        session
    }

    suspend fun refreshIfNeeded(): SupabaseAuthSession? = withContext(Dispatchers.IO) {
        val session = activeSession ?: readStoredSession()?.also { activeSession = it } ?: return@withContext null
        if (session.expiresAtEpochMs > System.currentTimeMillis() + 90_000L) return@withContext session
        try {
            val refreshed = postAuth(
                path = "token?grant_type=refresh_token",
                body = JSONObject().put("refresh_token", session.supabaseRefreshToken)
            )
            val updated = session.copy(
                supabaseAccessTokenJwt = refreshed.getString("access_token"),
                supabaseRefreshToken = refreshed.optString("refresh_token", session.supabaseRefreshToken),
                expiresAtEpochMs = refreshed.optLong("expires_at")
                    .takeIf { it > 0L }?.times(1000L)
                    ?: (System.currentTimeMillis() + refreshed.optLong("expires_in", 3600L) * 1000L)
            )
            activeSession = updated
            writeStoredSession(updated)
            updated
        } catch (_: Exception) {
            clearStoredSession()
            activeSession = null
            null
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = refreshIfNeeded()
        if (session != null) {
            val request = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/logout")
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer ${session.supabaseAccessTokenJwt}")
                .post("".toRequestBody(null))
                .build()
            runCatching { http.newCall(request).execute().use { } }
        }
        activeSession = null
        clearStoredSession()
    }

    suspend fun updateProfile(session: SupabaseAuthSession, username: String, displayName: String) = withContext(Dispatchers.IO) {
        val cleanUsername = username.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(32)
        require(cleanUsername.length >= 3) { "Username must be at least 3 characters." }
        val profile = JSONObject().put("id", session.userIdUuid).put("username", cleanUsername)
            .put("display_name", displayName.trim().take(80).ifBlank { "JoyTube User" })
        session.avatarUrl?.let { profile.put("avatar_url", it) }
        restUpsert("profiles", profile, session.supabaseAccessTokenJwt, "resolution=merge-duplicates,return=minimal")
    }

    private fun ensureProfileAndPreferences(session: SupabaseAuthSession) {
        val profile = JSONObject()
            .put("id", session.userIdUuid)
            .put("username", session.username)
            .put("display_name", session.displayName.take(80).ifBlank { "JoyTube User" })
        session.avatarUrl?.let { profile.put("avatar_url", it) }
        restUpsert("profiles", profile, session.supabaseAccessTokenJwt, "resolution=merge-duplicates,return=minimal")

        val preferences = JSONObject()
            .put("user_id", session.userIdUuid)
            .put("settings", JSONObject())
            .put("theme", "glass")
        restUpsert("user_preferences", preferences, session.supabaseAccessTokenJwt, "resolution=ignore-duplicates,return=minimal")
    }

    private fun restUpsert(table: String, body: JSONObject, token: String, prefer: String) {
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/$table?on_conflict=${if (table == "profiles") "id" else "user_id"}")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Prefer", prefer)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val details = response.body?.string().orEmpty().take(400)
                throw IllegalStateException("Saving the JoyTube profile failed (${response.code}): $details")
            }
        }
    }

    private fun postAuth(path: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/$path")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        return http.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(responseBody) }.getOrNull()
                val description = firstNonBlank(
                    error?.optString("msg").orEmpty(),
                    error?.optString("message").orEmpty(),
                    "Google sign-in failed. Check the Google provider configuration and try again."
                )
                throw IllegalStateException(description)
            }
            JSONObject(responseBody)
        }
    }

    private fun writeStoredSession(session: SupabaseAuthSession) {
        val context = appContext ?: return
        val json = JSONObject()
            .put("user_id", session.userIdUuid)
            .put("email", session.email)
            .put("username", session.username)
            .put("display_name", session.displayName)
            .put("avatar_url", session.avatarUrl)
            .put("access_token", session.supabaseAccessTokenJwt)
            .put("refresh_token", session.supabaseRefreshToken)
            .put("expires_at", session.expiresAtEpochMs)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(json.toString().toByteArray(StandardCharsets.UTF_8))
        val payload = cipher.iv + cipherText
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(SESSION_KEY, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    private fun readStoredSession(): SupabaseAuthSession? {
        val context = appContext ?: return null
        return try {
            val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(SESSION_KEY, null) ?: return null
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, 12)
            val encrypted = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(encrypted), StandardCharsets.UTF_8))
            SupabaseAuthSession(
                userIdUuid = json.getString("user_id"),
                email = json.getString("email"),
                username = json.getString("username"),
                displayName = json.getString("display_name"),
                avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() },
                supabaseAccessTokenJwt = json.getString("access_token"),
                supabaseRefreshToken = json.getString("refresh_token"),
                expiresAtEpochMs = json.getLong("expires_at")
            )
        } catch (_: Exception) {
            clearStoredSession()
            null
        }
    }

    private fun clearStoredSession() {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.remove(SESSION_KEY)?.apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun usernameFor(email: String, userId: String): String {
        val base = email.substringBefore('@').lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .take(24)
            .ifBlank { "joytube_user" }
        return "${base}_${userId.filter { it.isLetterOrDigit() }.takeLast(6).lowercase()}"
    }

    private fun randomNonce(): String = ByteArray(32).also(SecureRandom()::nextBytes)
        .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun firstNonBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() } ?: ""

    private fun firstNonBlankOrNull(vararg values: String): String? =
        values.firstOrNull { it.isNotBlank() }

    companion object {
        private const val PREFS_NAME = "joytube_auth_session"
        private const val SESSION_KEY = "encrypted_session"
        private const val KEY_ALIAS = "joytube_supabase_session_v1"
    }
}

object SupabaseAuthRuntime {
    val manager = SupabaseGoogleAuthManager()
}
