package com.cznwiki.app.network

import android.content.Context
import android.util.Log
import com.cznwiki.app.data.LocalDataManager
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Remote update manager for syncing wiki data from a remote source.
 *
 * Architecture:
 * - Remote repo (e.g., GitHub) hosts version.json + data JSON files
 * - App checks version.json on startup (silent background) or manual trigger
 * - If remote version > local version, downloads new data files
 * - Imports downloaded data into Room database
 *
 * Enhanced features:
 * - Silent background check on startup (no UI popup)
 * - Exponential backoff retry (max 3 attempts) on failure
 * - Fine-grained download progress callbacks (per-file byte tracing)
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
        private const val KEY_LAST_SILENT_CHECK = "last_silent_check_time"

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val BASE_RETRY_DELAY_MS = 2_000L   // 2s base
        private const val MAX_RETRY_DELAY_MS = 30_000L   // 30s cap

        // Silent check interval: only perform silent check once per hour
        private const val SILENT_CHECK_INTERVAL_MS = 3_600_000L

        // Base URL for remote data repo
        private const val BASE_URL = "https://raw.githubusercontent.com/Nandy5474/czn-wiki-app/main/data/"
        private const val VERSION_URL = "${BASE_URL}version.json"
        private const val CHARACTERS_URL = "${BASE_URL}characters.json"
        private const val CARDS_URL = "${BASE_URL}cards.json"
        private const val SELF_AWARENESS_URL = "${BASE_URL}self_awareness.json"
        private const val USER_COLLECTION_URL = "${BASE_URL}user_collection.json"
        private const val EVENTS_URL = "${BASE_URL}events.json"
        private const val BANNERS_URL = "${BASE_URL}banners.json"

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

    // ===================== Progress Callback =====================

    /**
     * Fine-grained download progress information.
     * @param fileLabel    Human-readable file name (e.g. "角色数据")
     * @param bytesRead    Bytes downloaded so far
     * @param totalBytes   Expected total bytes (-1 if unknown)
     * @param filesDone    Number of files fully downloaded
     * @param totalFiles   Total number of files to download
     * @param stage        Current stage description
     */
    data class DownloadProgress(
        val fileLabel: String,
        val bytesRead: Long,
        val totalBytes: Long,
        val filesDone: Int,
        val totalFiles: Int,
        val stage: String
    )

    data class RemoteVersion(
        val version: String = "0",
        val version_code: Int = 0,
        val characters_url: String = CHARACTERS_URL,
        val cards_url: String = CARDS_URL,
        val self_awareness_url: String = SELF_AWARENESS_URL,
        val user_collection_url: String = USER_COLLECTION_URL,
        val events_url: String = EVENTS_URL,
        val banners_url: String = BANNERS_URL,
        val update_date: String = "",
        val changelog: String = ""
    )

    data class UpdateResult(
        val success: Boolean,
        val message: String,
        val version: Int = 0,
        val remoteVersion: String = "",
        val update_date: String = "",
        val charsUpdated: Int = 0,
        val cardsUpdated: Int = 0,
        val saUpdated: Int = 0,
        val userCollUpdated: Int = 0,
        val eventsUpdated: Int = 0,
        val bannersUpdated: Int = 0
    )

    /** Get current local data version */
    fun getLocalVersion(): Int = prefs.getInt(KEY_DATA_VERSION, 0)

    /** Set local data version */
    private fun setLocalVersion(version: Int) {
        prefs.edit().putInt(KEY_DATA_VERSION, version).apply()
    }

    sealed class UpdateStatus {
        data object Checking : UpdateStatus()
        data class Downloading(val step: String) : UpdateStatus()
        data class Progress(val progress: DownloadProgress) : UpdateStatus()
        data class Done(val result: UpdateResult) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    // ===================== Silent Background Check =====================

    /**
     * Perform a silent background update check on app startup.
     * - Does NOT show any UI dialogs to the user
     * - Respects a minimum interval (SILENT_CHECK_INTERVAL_MS) to avoid excessive network calls
     * - On failure: silently retries up to MAX_RETRIES with exponential backoff
     * - On success (update found): performs full data sync silently
     *
     * Call this from Application.onCreate() or main Activity onCreate().
     */
    fun startSilentBackgroundCheck() {
        scope.launch {
            val lastCheck = prefs.getLong(KEY_LAST_SILENT_CHECK, 0L)
            val now = System.currentTimeMillis()
            if (now - lastCheck < SILENT_CHECK_INTERVAL_MS) {
                Log.d(TAG, "Silent check skipped: last check was ${(now - lastCheck) / 1000}s ago")
                return@launch
            }

            prefs.edit().putLong(KEY_LAST_SILENT_CHECK, now).apply()
            Log.i(TAG, "Starting silent background update check...")

            val result = checkForUpdateWithRetry(onStatus = { /* silent — no UI callback */ })
            if (result.success) {
                Log.i(TAG, "Silent check done: ${result.message}")
            } else {
                Log.w(TAG, "Silent check failed after retries: ${result.message}")
            }
        }
    }

    // ===================== Retry with Exponential Backoff =====================

    /**
     * Wraps checkForUpdate with exponential-backoff retry logic.
     * On failure, retries up to MAX_RETRIES times with delays: 2s, 4s, 8s, ...
     * capped at MAX_RETRY_DELAY_MS.
     *
     * @param onStatus  Optional UI callback (not used during silent checks).
     */
    private suspend fun checkForUpdateWithRetry(
        onStatus: (UpdateStatus) -> Unit = {}
    ): UpdateResult {
        var lastResult: UpdateResult? = null

        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = min(
                    (BASE_RETRY_DELAY_MS * 2.0.pow(attempt - 1)).toLong(),
                    MAX_RETRY_DELAY_MS
                )
                Log.i(TAG, "Retry attempt $attempt/$MAX_RETRIES after ${delayMs}ms backoff")
                delay(delayMs)
            }

            try {
                val result = checkForUpdate(onStatus)
                if (result.success) return result
                lastResult = result
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed", e)
                lastResult = UpdateResult(false, "尝试 $attempt 失败: ${e.message}")
            }
        }

        return lastResult ?: UpdateResult(false, "更新失败: 已重试 $MAX_RETRIES 次")
    }

    // ===================== Main Update Check =====================

    /** Check if update is available (non-blocking, returns via callback).
     *  With progress callback support for fine-grained download tracking. */
    suspend fun checkForUpdate(onStatus: (UpdateStatus) -> Unit = {}): UpdateResult = withContext(Dispatchers.IO) {
        try {
            onStatus(UpdateStatus.Checking)
            val versionJson = fetchUrl(VERSION_URL) ?: return@withContext UpdateResult(
                false, "无法连接到更新服务器"
            )

            val remoteVersion: RemoteVersion = gson.fromJson(versionJson, RemoteVersion::class.java)
            val localVersion = getLocalVersion()

            // Compare using version_code (integer)
            val remoteVerNum = remoteVersion.version_code
            if (remoteVerNum <= localVersion) {
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                return@withContext UpdateResult(true, "数据已是最新", localVersion)
            }

            // === 远程更新前：保存用户修改到 LocalDataManager ===
            val localDataMgr = LocalDataManager.getInstance(context)
            val savedCount = localDataMgr.snapshotUserModsBeforeRemoteUpdate(database)
            if (savedCount > 0) {
                Log.i(TAG, "Saved $savedCount user collection entries before remote update")
            }

            // Build download list
            data class DownloadTask(val label: String, val url: String)
            val tasks = listOf(
                DownloadTask("角色数据", remoteVersion.characters_url),
                DownloadTask("卡牌数据", remoteVersion.cards_url),
                DownloadTask("命座数据", remoteVersion.self_awareness_url),
                DownloadTask("活动数据", remoteVersion.events_url),
                DownloadTask("卡池数据", remoteVersion.banners_url)
            )
            val totalFiles = tasks.size
            var filesDone = 0

            // Download and import new data
            var charsUpdated = 0
            var cardsUpdated = 0
            var saUpdated = 0
            var eventsUpdated = 0
            var bannersUpdated = 0

            // Download characters
            try {
                onStatus(UpdateStatus.Downloading("角色数据"))
                val charsJson = fetchUrlWithProgress(
                    remoteVersion.characters_url,
                    onStatus, "角色数据", 0, totalFiles
                )
                filesDone++
                if (charsJson != null) {
                    val chars: List<CharacterEntity> = gson.fromJson(
                        charsJson, object : TypeToken<List<CharacterEntity>>() {}.type
                    )
                    database.characterDao().insertAll(chars)
                    charsUpdated = chars.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update characters", e)
                filesDone++
            }

            // Download cards
            try {
                onStatus(UpdateStatus.Downloading("卡牌数据"))
                val cardsJson = fetchUrlWithProgress(
                    remoteVersion.cards_url,
                    onStatus, "卡牌数据", 1, totalFiles
                )
                filesDone++
                if (cardsJson != null) {
                    val cards: List<CardEntity> = gson.fromJson(
                        cardsJson, object : TypeToken<List<CardEntity>>() {}.type
                    )
                    database.cardDao().insertAll(cards)
                    cardsUpdated = cards.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update cards", e)
                filesDone++
            }

            // Download self-awareness
            try {
                onStatus(UpdateStatus.Downloading("命座数据"))
                val saJson = fetchUrlWithProgress(
                    remoteVersion.self_awareness_url,
                    onStatus, "命座数据", 2, totalFiles
                )
                filesDone++
                if (saJson != null) {
                    val saList: List<SelfAwarenessEntity> = gson.fromJson(
                        saJson, object : TypeToken<List<SelfAwarenessEntity>>() {}.type
                    )
                    database.selfAwarenessDao().insertAll(saList)
                    saUpdated = saList.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update self-awareness", e)
                filesDone++
            }

            // Download events
            try {
                onStatus(UpdateStatus.Downloading("活动数据"))
                val eventsJson = fetchUrlWithProgress(
                    remoteVersion.events_url,
                    onStatus, "活动数据", 3, totalFiles
                )
                filesDone++
                if (eventsJson != null) {
                    val events: List<EventEntity> = gson.fromJson(
                        eventsJson, object : TypeToken<List<EventEntity>>() {}.type
                    )
                    database.eventDao().insertAll(events)
                    eventsUpdated = events.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update events", e)
                filesDone++
            }

            // Download banners
            try {
                onStatus(UpdateStatus.Downloading("卡池数据"))
                val bannersJson = fetchUrlWithProgress(
                    remoteVersion.banners_url,
                    onStatus, "卡池数据", 4, totalFiles
                )
                filesDone++
                if (bannersJson != null) {
                    val banners: List<BannerEntity> = gson.fromJson(
                        bannersJson, object : TypeToken<List<BannerEntity>>() {}.type
                    )
                    database.bannerDao().insertAll(banners)
                    bannersUpdated = banners.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update banners", e)
                filesDone++
            }

            // Notify final progress: all files done (success or fail)
            onStatus(UpdateStatus.Progress(DownloadProgress(
                fileLabel = "完成",
                bytesRead = 0,
                totalBytes = 0,
                filesDone = totalFiles,
                totalFiles = totalFiles,
                stage = "import"
            )))

            // === 远程更新后：回灌用户修改到 Room ===
            // 注意：不再从远程下载 user_collection.json 覆盖用户数据
            localDataMgr.reapplyUserModsAfterRemoteUpdate(database)

            setLocalVersion(remoteVerNum)
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            UpdateResult(
                success = true,
                message = remoteVersion.changelog.ifBlank { "数据已更新到 v${remoteVersion.version}" },
                version = remoteVerNum,
                remoteVersion = remoteVersion.version,
                charsUpdated = charsUpdated,
                cardsUpdated = cardsUpdated,
                saUpdated = saUpdated,
                userCollUpdated = savedCount,
                eventsUpdated = eventsUpdated,
                bannersUpdated = bannersUpdated,
                update_date = remoteVersion.update_date
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

    // ===================== Networking =====================

    /**
     * Fetch URL with fine-grained progress tracking.
     * Reads response body in 8KB chunks, reporting bytes read vs. content-length.
     * Falls back gracefully if content-length is unknown (totalBytes = -1).
     */
    private fun fetchUrlWithProgress(
        url: String,
        onStatus: (UpdateStatus) -> Unit,
        fileLabel: String,
        filesDone: Int,
        totalFiles: Int
    ): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for $url")
                    return null
                }

                val body = response.body ?: return null
                val contentLength = body.contentLength()
                val source: BufferedSource = body.source()
                val buffer = Buffer()
                var bytesRead = 0L

                while (!source.exhausted()) {
                    val read = source.read(buffer, 8192)
                    if (read == -1L) break
                    bytesRead += read
                    onStatus(UpdateStatus.Progress(DownloadProgress(
                        fileLabel = fileLabel,
                        bytesRead = bytesRead,
                        totalBytes = contentLength,
                        filesDone = filesDone,
                        totalFiles = totalFiles,
                        stage = "downloading"
                    )))
                }

                buffer.readUtf8()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch failed: $url", e)
            null
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
private suspend fun seedDatabaseFromAssets(context: Context, database: AppDatabase) {
    com.cznwiki.app.data.database.seedDatabaseFromAssets(context, database)
}
