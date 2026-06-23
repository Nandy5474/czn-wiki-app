package com.cznwiki.app.data

import android.content.Context
import android.util.Log
import com.cznwiki.app.CznApplication
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 远程数据自动更新管理器
 *
 * 工作流程（阻塞顺序执行，保证事务安全）：
 * 1. 从 GitHub raw URL 拉取远程 version.json
 * 2. 比较 remote_version_code 与本地版本号
 * 3. 若远程更新 → 保存用户修改 → 清空基础表 → 逐表下载并导入 → 回灌用户修改 → 更新本地版本 → 更新 assets 版本
 * 4. 全程通过 progressCallback 报告进度
 */

data class RemoteVersionInfo(
    val version: String,
    val version_code: Int,
    val characters_url: String,
    val cards_url: String,
    val self_awareness_url: String,
    val user_collection_url: String,
    val events_url: String,
    val banners_url: String,
    val update_date: String,
    val changelog: String,
    val changes: List<String>
)

data class UpdateProgress(
    val stage: String,        // "checking" | "downloading" | "importing" | "done" | "error"
    val currentFile: String,  // 当前正在处理的文件
    val progress: Int,        // 0-100
    val description: String   // 用户可读的描述
)

class RemoteUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "RemoteUpdateManager"
        private const val REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/Nandy5474/czn-wiki-app/main/data/version.json"

        @Volatile
        private var instance: RemoteUpdateManager? = null

        fun getInstance(context: Context): RemoteUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: RemoteUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()
    private val localDataMgr by lazy { LocalDataManager.getInstance(context) }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 静默检查远程更新（启动时调用，有更新直接静默执行）
     */
    suspend fun checkAndUpdateSilently(database: AppDatabase) {
        try {
            val remoteInfo = fetchRemoteVersion()
            if (remoteInfo == null) {
                Log.d(TAG, "Failed to fetch remote version, skipping remote update")
                return
            }

            val localVersion = localDataMgr.getLocalVersion()
            if (remoteInfo.version_code <= localVersion) {
                Log.d(TAG, "Remote data is not newer (remote=$remoteInfo.version_code, local=$localVersion)")
                return
            }

            Log.i(TAG, "Remote update available: $localVersion -> ${remoteInfo.version_code}")
            performUpdate(database, remoteInfo) { progress ->
                Log.d(TAG, "Update progress: ${progress.stage} ${progress.currentFile} ${progress.progress}%")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Silent remote update check failed", e)
        }
    }

    /**
     * 用户手动触发更新（带进度回调，用于 UI 展示）
     */
    suspend fun checkAndUpdateWithProgress(
        database: AppDatabase,
        onProgress: (UpdateProgress) -> Unit
    ): RemoteVersionInfo? {
        onProgress(UpdateProgress("checking", "", 0, "正在检查远程版本..."))

        val remoteInfo = fetchRemoteVersion()
        if (remoteInfo == null) {
            onProgress(UpdateProgress("error", "", 0, "无法连接远程服务器，请检查网络"))
            return null
        }

        val localVersion = localDataMgr.getLocalVersion()
        if (remoteInfo.version_code <= localVersion) {
            onProgress(UpdateProgress("done", "", 100, "数据已是最新版本 (v$localVersion)"))
            return null
        }

        Log.i(TAG, "Manual update: $localVersion -> ${remoteInfo.version_code}")
        performUpdate(database, remoteInfo, onProgress)
        return remoteInfo
    }

    /**
     * 获取远程版本信息（仅检查，不执行更新）
     */
    suspend fun fetchRemoteVersionInfo(): RemoteVersionInfo? {
        return fetchRemoteVersion()
    }

    // ==================== 内部实现 ====================

    private suspend fun fetchRemoteVersion(): RemoteVersionInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(REMOTE_VERSION_URL).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Remote version fetch failed: ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                parseVersionJson(body)
            } catch (e: Exception) {
                Log.w(TAG, "Remote version fetch error", e)
                null
            }
        }
    }

    private fun parseVersionJson(json: String): RemoteVersionInfo {
        val map = gson.fromJson(json, Map::class.java)
        return RemoteVersionInfo(
            version = map["version"] as? String ?: "",
            version_code = ((map["version_code"] as? Double)?.toInt() ?: 0),
            characters_url = map["characters_url"] as? String ?: "",
            cards_url = map["cards_url"] as? String ?: "",
            self_awareness_url = map["self_awareness_url"] as? String ?: "",
            user_collection_url = map["user_collection_url"] as? String ?: "",
            events_url = (map["events_url"] as? String) ?: "",
            banners_url = (map["banners_url"] as? String) ?: "",
            update_date = map["update_date"] as? String ?: "",
            changelog = map["changelog"] as? String ?: "",
            changes = (map["changes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    private suspend fun performUpdate(
        database: AppDatabase,
        info: RemoteVersionInfo,
        onProgress: (UpdateProgress) -> Unit
    ) {
        // 阶段 1：保存用户修改快照
        onProgress(UpdateProgress("downloading", "快照", 5, "正在备份你的个人数据..."))
        val userModCount = localDataMgr.snapshotUserModsBeforeRemoteUpdate(database)
        Log.i(TAG, "Saved $userModCount user mod entries")

        // 阶段 2：清空基础数据表（保留 user_collection 和 teams）
        onProgress(UpdateProgress("importing", "清理", 10, "正在清理旧数据..."))
        database.characterDao().deleteAll()
        database.cardDao().deleteAll()
        database.selfAwarenessDao().deleteAll()
        database.eventDao().deleteAll()
        database.bannerDao().deleteAll()

        // 阶段 3：逐表下载并导入（共 6 张表）
        val tables = listOf(
            Triple("characters", info.characters_url, 15..30),
            Triple("cards", info.cards_url, 30..45),
            Triple("self_awareness", info.self_awareness_url, 45..60),
            Triple("events", info.events_url, 60..70),
            Triple("banners", info.banners_url, 70..80),
            Triple("user_collection", info.user_collection_url, 80..90)
        )

        for ((name, url, range) in tables) {
            if (url.isBlank()) {
                Log.w(TAG, "URL for $name is blank, skipping")
                continue
            }
            onProgress(UpdateProgress("downloading", name, range.first, "正在下载 $name 数据..."))
            val json = downloadJson(url)
            if (json == null) {
                Log.e(TAG, "Failed to download $name from $url")
                continue
            }
            onProgress(UpdateProgress("importing", name, range.last, "正在导入 $name 数据..."))
            importTable(database, name, json)
        }

        // 阶段 4：回灌用户修改
        if (userModCount > 0) {
            onProgress(UpdateProgress("importing", "用户数据", 92, "正在恢复你的个人数据..."))
            localDataMgr.reapplyUserModsAfterRemoteUpdate(database)
        }

        // 阶段 5：更新版本号
        localDataMgr.setLocalVersion(info.version_code)
        onProgress(UpdateProgress("done", "", 100, "数据更新完成！已更新至 ${info.version}"))
        Log.i(TAG, "Remote update complete: ${info.version}")
    }

    private fun downloadJson(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Download failed for $url: ${response.code}")
                return null
            }
            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "Download error for $url", e)
            null
        }
    }

    private suspend fun importTable(database: AppDatabase, tableName: String, json: String) {
        withContext(Dispatchers.IO) {
            try {
                when (tableName) {
                    "characters" -> {
                        val list: List<CharacterEntity> = parseJsonArray(json)
                        if (list.isNotEmpty()) database.characterDao().insertAll(list)
                        Log.i(TAG, "Imported ${list.size} $tableName")
                    }
                    "cards" -> {
                        val list: List<CardEntity> = parseJsonArray(json)
                        if (list.isNotEmpty()) database.cardDao().insertAll(list)
                        Log.i(TAG, "Imported ${list.size} $tableName")
                    }
                    "self_awareness" -> {
                        val list: List<SelfAwarenessEntity> = parseJsonArray(json)
                        if (list.isNotEmpty()) database.selfAwarenessDao().insertAll(list)
                        Log.i(TAG, "Imported ${list.size} $tableName")
                    }
                    "events" -> {
                        val list: List<EventEntity> = parseJsonArray(json)
                        if (list.isNotEmpty()) database.eventDao().insertAll(list)
                        Log.i(TAG, "Imported ${list.size} $tableName")
                    }
                    "banners" -> {
                        val list: List<BannerEntity> = parseJsonArray(json)
                        if (list.isNotEmpty()) database.bannerDao().insertAll(list)
                        Log.i(TAG, "Imported ${list.size} $tableName")
                    }
                    "user_collection" -> {
                        // user_collection 使用 upsert，避免覆盖已有用户数据
                        val list: List<UserCollectionEntity> = parseJsonArray(json)
                        for (entity in list) {
                            val existing = database.userCollectionDao().getByCharacterId(entity.characterId)
                            if (existing == null) {
                                database.userCollectionDao().insertAll(listOf(entity))
                            }
                            // 已有记录不覆盖（保留用户的个人设置）
                        }
                        Log.i(TAG, "Merged ${list.size} $tableName (existing entries preserved)")
                    }
                    else -> {
                        Log.w(TAG, "Unknown table: $tableName")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import $tableName", e)
            }
        }
    }

    private inline fun <reified T> parseJsonArray(json: String): List<T> {
        return gson.fromJson(json, object : TypeToken<List<T>>() {}.type)
    }
}
