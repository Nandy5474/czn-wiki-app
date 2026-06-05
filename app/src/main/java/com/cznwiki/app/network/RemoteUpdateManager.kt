package com.cznwiki.app.network

import android.content.Context
import android.util.Log
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Remote update manager for syncing wiki data from a remote source.
 *
 * Architecture:
 * - Remote repo (e.g., GitHub) hosts version.json + data JSON files
 * - App checks version.json on startup or manual trigger
 * - If remote version > local version, downloads new data files
 * - Imports downloaded data into Room database
 */
class RemoteUpdateManager(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "RemoteUpdate"
        private const val PREFS_NAME = "czn_remote_update"
        private const val KEY_DATA_VERSION = "data_version"
        private const val KEY_LAST_CHECK = "last_check_time"

        // Base URL for remote data repo
        private const val BASE_URL = "https://raw.githubusercontent.com/Nandy5474/czn-wiki-data/main/"
        private const val VERSION_URL = "${BASE_URL}version.json"
        private const val CHARACTERS_URL = "${BASE_URL}characters.json"
        private const val CARDS_URL = "${BASE_URL}cards.json"
        private const val SELF_AWARENESS_URL = "${BASE_URL}self_awareness.json"
        private const val USER_COLLECTION_URL = "${BASE_URL}user_collection.json"

        @Volatile
        private var instance: RemoteUpdateManager? = null

        fun getInstance(context: Context, database: AppDatabase): RemoteUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: RemoteUpdateManager(context.applicationContext, database).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class RemoteVersion(
        val version: Int = 0,
        val characters_url: String = CHARACTERS_URL,
        val cards_url: String = CARDS_URL,
        val self_awareness_url: String = SELF_AWARENESS_URL,
        val user_collection_url: String = USER_COLLECTION_URL,
        val update_date: String = "",
        val changelog: String = ""
    )

    data class UpdateResult(
        val success: Boolean,
        val message: String,
        val version: Int = 0,
        val charsUpdated: Int = 0,
        val cardsUpdated: Int = 0,
        val saUpdated: Int = 0,
        val userCollUpdated: Int = 0
    )

    /** Get current local data version */
    fun getLocalVersion(): Int = prefs.getInt(KEY_DATA_VERSION, 0)

    /** Set local data version */
    private fun setLocalVersion(version: Int) {
        prefs.edit().putInt(KEY_DATA_VERSION, version).apply()
    }

    /** Check if update is available (non-blocking, returns via callback) */
    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val versionJson = fetchUrl(VERSION_URL) ?: return@withContext UpdateResult(
                false, "无法连接到更新服务器"
            )

            val remoteVersion: RemoteVersion = gson.fromJson(versionJson, RemoteVersion::class.java)
            val localVersion = getLocalVersion()

            if (remoteVersion.version <= localVersion) {
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                return@withContext UpdateResult(true, "数据已是最新", localVersion)
            }

            // Download and import new data
            var charsUpdated = 0
            var cardsUpdated = 0
            var saUpdated = 0

            // Download characters
            try {
                val charsJson = fetchUrl(remoteVersion.characters_url)
                if (charsJson != null) {
                    val chars: List<CharacterEntity> = gson.fromJson(
                        charsJson, object : TypeToken<List<CharacterEntity>>() {}.type
                    )
                    database.characterDao().insertAll(chars)
                    charsUpdated = chars.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update characters", e)
            }

            // Download cards
            try {
                val cardsJson = fetchUrl(remoteVersion.cards_url)
                if (cardsJson != null) {
                    val cards: List<CardEntity> = gson.fromJson(
                        cardsJson, object : TypeToken<List<CardEntity>>() {}.type
                    )
                    database.cardDao().insertAll(cards)
                    cardsUpdated = cards.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update cards", e)
            }

            // Download self-awareness
            try {
                val saJson = fetchUrl(remoteVersion.self_awareness_url)
                if (saJson != null) {
                    val saList: List<SelfAwarenessEntity> = gson.fromJson(
                        saJson, object : TypeToken<List<SelfAwarenessEntity>>() {}.type
                    )
                    database.selfAwarenessDao().insertAll(saList)
                    saUpdated = saList.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update self-awareness", e)
            }

            // Download user_collection (seed data, first-time sync only)
            var userCollUpdated = 0
            try {
                val collectionJson = fetchUrl(remoteVersion.user_collection_url)
                if (collectionJson != null) {
                    val collections: List<UserCollectionEntity> = gson.fromJson(
                        collectionJson, object : TypeToken<List<UserCollectionEntity>>() {}.type
                    )
                    database.userCollectionDao().insertAll(collections)
                    userCollUpdated = collections.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update user collection", e)
            }

            setLocalVersion(remoteVersion.version)
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            UpdateResult(
                success = true,
                message = remoteVersion.changelog.ifBlank { "数据已更新到 v${remoteVersion.version}" },
                version = remoteVersion.version,
                charsUpdated = charsUpdated,
                cardsUpdated = cardsUpdated,
                saUpdated = saUpdated,
                userCollUpdated = userCollUpdated
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateResult(false, "更新失败: ${e.message}")
        }
    }

    /** Force re-import from bundled assets */
    suspend fun resetToBundledData(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val localVersion = getLocalVersion()
            // Reset version to force re-import
            setLocalVersion(0)

            // Re-import from assets
            seedDatabaseFromAssets(context, database)
            setLocalVersion(localVersion.coerceAtLeast(1))

            UpdateResult(true, "已重置为内置数据 v$localVersion", localVersion)
        } catch (e: Exception) {
            UpdateResult(false, "重置失败: ${e.message}")
        }
    }

    private fun fetchUrl(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "HTTP ${response.code} for $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch failed: $url", e)
            null
        }
    }
}

// Re-export seeding function
private fun seedDatabaseFromAssets(context: Context, database: AppDatabase) {
    // Call the existing function from AppDatabase.kt
    com.cznwiki.app.data.database.seedDatabaseFromAssets(context, database)
}
