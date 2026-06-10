package com.cznwiki.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.disk.DiskCache
import com.cznwiki.app.coil.AssetUriFetcher
import com.cznwiki.app.data.LocalDataManager
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.database.seedDatabaseFromAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class CznApplication : Application(), ImageLoaderFactory {
    val database by lazy { AppDatabase.getInstance(this) }
    val localDataManager by lazy { LocalDataManager.getInstance(this) }
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(20 * 1024 * 1024)
                    .build()
            }
            .components {
                add(AssetUriFetcher.Factory(this@CznApplication))
            }
            .crossfade(300)
            .allowHardware(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // 首次启动：同步从 assets 导入基础数据，等待完成后再继续
        if (localDataManager.getLocalVersion() == 0) {
            runBlocking(Dispatchers.IO) {
                seedDatabaseFromAssets(this@CznApplication, database)
            }
            localDataManager.setLocalVersion(localDataManager.getAssetsVersion())
        }
        // 安全网：如果数据库因 Destructive Migration 被清空，强制重新导入
        runBlocking(Dispatchers.IO) {
            val charCount = database.characterDao().getCount()
            if (charCount == 0 && localDataManager.getLocalVersion() > 0) {
                seedDatabaseFromAssets(this@CznApplication, database)
                localDataManager.setLocalVersion(localDataManager.getAssetsVersion())
            }
        }
        // 检查数据版本，版本变化时触发更新流程（保存用户修改 → 清空 → 导入 → 回灌）
        localDataManager.checkAndUpdateData(database, appScope)
    }
}
