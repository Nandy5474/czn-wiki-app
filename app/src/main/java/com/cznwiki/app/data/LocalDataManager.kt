package com.cznwiki.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.entity.UserCollectionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

/**
 * 数据分层架构管理器
 *
 * 三层架构：
 * 1. 基础数据层：assets/data/*.json（随 APK 发布，只读）
 * 2. 用户修改层：SharedPreferences，以 characterId 为 key 存储用户编辑
 * 3. 运行时数据层：Room 数据库中的合并后数据
 *
 * 工作流程：
 * - 首次启动：assets 基础数据 → Room 数据库 + 初始 user_collection
 * - 数据更新：保存用户修改 → 清空旧基础数据 → 导入新基础数据 → 回灌用户修改
 * - 用户编辑：更新 Room + 记录到用户修改层
 */
class LocalDataManager(context: Context) {
    companion object {
        private const val TAG = "LocalDataManager"
        private const val PREFS_NAME = "czn_local_data"
        private const val KEY_DATA_VERSION = "data_version"
        private const val KEY_USER_MODS = "user_modifications"

        @Volatile
        private var instance: LocalDataManager? = null

        fun getInstance(context: Context): LocalDataManager {
            return instance ?: synchronized(this) {
                instance ?: LocalDataManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ==================== 版本管理 ====================

    /** 从 assets 读取基础数据版本号 */
    fun getAssetsVersion(): Int {
        return try {
            val json = context.assets.open("data/version.json")
                .bufferedReader().use { it.readText() }
            val obj = gson.fromJson(json, Map::class.java)
            (obj["version_code"] as? Double)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read assets version", e)
            0
        }
    }

    /** 获取本地已存储的数据版本号（首次启动为 0） */
    fun getLocalVersion(): Int = prefs.getInt(KEY_DATA_VERSION, 0)

    /** 更新本地数据版本号 */
    fun setLocalVersion(version: Int) {
        prefs.edit().putInt(KEY_DATA_VERSION, version).apply()
    }

    // ==================== 用户修改层 ====================

    /**
     * 用户修改记录结构：
     * { characterId: { "customTier": "T0", "owned": true, "constellation": 3, ... } }
     */
    data class UserModEntry(
        val customTier: String? = null,
        val owned: Boolean? = null,
        val constellation: Int? = null,
        val potential: Int? = null,
        val partnerId: Int? = null
    )

    /** 读取所有用户修改记录 */
    fun getAllUserMods(): Map<Int, UserModEntry> {
        val json = prefs.getString(KEY_USER_MODS, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, UserModEntry>>() {}.type
            val raw: Map<String, UserModEntry> = gson.fromJson(json, type)
            raw.mapKeys { it.key.toInt() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse user mods", e)
            emptyMap()
        }
    }

    /**
     * 保存单个字段的用户修改，合并到已有记录
     */
    fun saveUserModification(
        characterId: Int,
        field: String,
        value: Any?
    ) {
        val allMods = getAllUserMods().toMutableMap()
        val existing = allMods[characterId] ?: UserModEntry()

        val updated = when (field) {
            "customTier" -> existing.copy(customTier = value as? String)
            "owned" -> existing.copy(owned = value as? Boolean)
            "constellation" -> existing.copy(constellation = value as? Int)
            "potential" -> existing.copy(potential = value as? Int)
            "partnerId" -> existing.copy(partnerId = value as? Int)
            else -> {
                Log.w(TAG, "Unknown field: $field")
                existing
            }
        }

        allMods[characterId] = updated
        persistUserMods(allMods)
    }

    /** 批量保存整个 user_collection 记录作为用户修改快照 */
    fun saveAllUserMods(entities: List<UserCollectionEntity>) {
        val allMods = entities.associate { uc ->
            uc.characterId to UserModEntry(
                customTier = uc.customTier.takeIf { it.isNotBlank() },
                owned = uc.owned.takeIf { it },
                constellation = uc.constellation.takeIf { it > 0 },
                potential = uc.potential.takeIf { it > 0 },
                partnerId = uc.partnerId
            )
        }
        persistUserMods(allMods)
    }

    /** 清空用户修改记录 */
    fun clearUserMods() {
        prefs.edit().remove(KEY_USER_MODS).apply()
    }

    private fun persistUserMods(mods: Map<Int, UserModEntry>) {
        val stringMap = mods.mapKeys { it.key.toString() }
        val json = gson.toJson(stringMap)
        prefs.edit().putString(KEY_USER_MODS, json).apply()
    }

    // ==================== 数据更新流程 ====================

    /**
     * 检查并执行数据更新（在 CznApplication.onCreate 中调用）。
     * 如果 assets 版本 > 本地版本，触发完整更新流程。
     */
    fun checkAndUpdateData(database: AppDatabase, scope: CoroutineScope) {
        val assetsVersion = getAssetsVersion()
        val localVersion = getLocalVersion()

        if (assetsVersion <= localVersion && localVersion > 0) {
            Log.d(TAG, "Data up to date: local=$localVersion, assets=$assetsVersion")
            return
        }

        Log.i(TAG, "Data update needed: local=$localVersion -> assets=$assetsVersion")
        scope.launch(Dispatchers.IO) {
            try {
                performDataUpdate(database, assetsVersion)
            } catch (e: Exception) {
                Log.e(TAG, "Data update failed", e)
            }
        }
    }

    /**
     * 执行数据更新：
     * 1. 保存当前用户修改到 SharedPreferences
     * 2. 清空基础数据表（characters/cards/self_awareness）
     * 3. 从 assets 重新导入基础数据
     * 4. 将用户修改回灌到 Room
     * 5. 更新版本号
     */
    private suspend fun performDataUpdate(database: AppDatabase, newVersion: Int) {
        val localVersion = getLocalVersion()

        // 阶段 1：保存用户修改（非首次启动时）
        val userMods = if (localVersion > 0) {
            Log.i(TAG, "Phase 1: Saving user modifications...")
            val entities = database.userCollectionDao().getAllSync()
            saveAllUserMods(entities)
            getAllUserMods()
        } else {
            emptyMap()
        }

        // 阶段 2：清空基础数据表
        if (localVersion > 0) {
            Log.i(TAG, "Phase 2: Clearing base data...")
            database.characterDao().deleteAll()
            database.cardDao().deleteAll()
            database.selfAwarenessDao().deleteAll()
        }

        // 阶段 3：从 assets 重新导入基础数据
        Log.i(TAG, "Phase 3: Importing new base data from assets...")
        seedBaseDataFromAssets(database)

        // 阶段 4：回灌用户修改
        if (userMods.isNotEmpty()) {
            Log.i(TAG, "Phase 4: Re-applying user modifications (${userMods.size} entries)...")
            reapplyUserMods(database, userMods)
        }

        // 阶段 5：更新版本号
        setLocalVersion(newVersion)
        Log.i(TAG, "Data update complete. New version: $newVersion")
    }

    /** 从 assets 导入基础数据（characters/cards/self_awareness） */
    private suspend fun seedBaseDataFromAssets(database: AppDatabase) {
        // Characters
        try {
            val charJson = context.assets.open("data/characters.json")
                .bufferedReader().use { it.readText() }
            val charList: List<com.cznwiki.app.data.entity.CharacterEntity> = gson.fromJson(
                charJson,
                object : TypeToken<List<com.cznwiki.app.data.entity.CharacterEntity>>() {}.type
            )
            if (charList.isNotEmpty()) database.characterDao().insertAll(charList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import characters", e)
        }

        // Cards
        try {
            val cardsJson = context.assets.open("data/cards.json")
                .bufferedReader().use { it.readText() }
            val cardList: List<com.cznwiki.app.data.entity.CardEntity> = gson.fromJson(
                cardsJson,
                object : TypeToken<List<com.cznwiki.app.data.entity.CardEntity>>() {}.type
            )
            if (cardList.isNotEmpty()) database.cardDao().insertAll(cardList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import cards", e)
        }

        // Self Awareness
        try {
            val saJson = context.assets.open("data/self_awareness.json")
                .bufferedReader().use { it.readText() }
            val saList: List<com.cznwiki.app.data.entity.SelfAwarenessEntity> = gson.fromJson(
                saJson,
                object : TypeToken<List<com.cznwiki.app.data.entity.SelfAwarenessEntity>>() {}.type
            )
            if (saList.isNotEmpty()) database.selfAwarenessDao().insertAll(saList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import self awareness", e)
        }

        // User Collection (seed data, only on first launch)
        if (getLocalVersion() == 0) {
            try {
                val ucJson = context.assets.open("data/user_collection.json")
                    .bufferedReader().use { it.readText() }
                val ucList: List<UserCollectionEntity> = gson.fromJson(
                    ucJson,
                    object : TypeToken<List<UserCollectionEntity>>() {}.type
                )
                if (ucList.isNotEmpty()) database.userCollectionDao().insertAll(ucList)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import user collection seed", e)
            }
        }
    }

    /** 将用户修改回灌到 Room 数据库 */
    private suspend fun reapplyUserMods(
        database: AppDatabase,
        userMods: Map<Int, UserModEntry>
    ) {
        for ((characterId, mod) in userMods) {
            val existing = database.userCollectionDao().getByCharacterId(characterId)
            val entity = if (existing != null) {
                existing.copy(
                    customTier = mod.customTier ?: existing.customTier,
                    owned = mod.owned ?: existing.owned,
                    constellation = mod.constellation ?: existing.constellation,
                    potential = mod.potential ?: existing.potential,
                    partnerId = mod.partnerId ?: existing.partnerId
                )
            } else {
                UserCollectionEntity(
                    characterId = characterId,
                    owned = mod.owned ?: false,
                    constellation = mod.constellation ?: 0,
                    potential = mod.potential ?: 0,
                    partnerId = mod.partnerId,
                    customTier = mod.customTier ?: ""
                )
            }
            database.userCollectionDao().upsert(entity)
        }
    }

    // ==================== 远程更新前保存用户修改 ====================

    /**
     * 在远程更新前保存当前 user_collection 快照到用户修改层。
     * 返回保存的修改条目数。
     */
    suspend fun snapshotUserModsBeforeRemoteUpdate(database: AppDatabase): Int {
        val entities = database.userCollectionDao().getAllSync()
        saveAllUserMods(entities)
        return entities.size
    }

    /**
     * 远程更新后将用户修改回灌到 Room。
     */
    suspend fun reapplyUserModsAfterRemoteUpdate(database: AppDatabase) {
        val userMods = getAllUserMods()
        if (userMods.isNotEmpty()) {
            Log.i(TAG, "Re-applying ${userMods.size} user mods after remote update")
            reapplyUserMods(database, userMods)
        }
    }
}
