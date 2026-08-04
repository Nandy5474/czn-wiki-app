package com.cznwiki.app

import android.app.Application
import android.os.Environment
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.disk.DiskCache
import com.cznwiki.app.coil.AssetUriFetcher
import com.cznwiki.app.data.LocalDataManager
import com.cznwiki.app.network.RemoteUpdateManager
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.database.seedDatabaseFromAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CznApplication : Application(), ImageLoaderFactory {
    val database by lazy { AppDatabase.getInstance(this) }
    val localDataManager by lazy { LocalDataManager.getInstance(this) }
    val remoteUpdateManager by lazy { RemoteUpdateManager.getInstance(this, database) }
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
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
            crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nThread: ${thread.name}\n${e.stackTraceToString()}\n---\n")
        }
        // Block 1: 首次启动数据导入（关键）
        try {
            if (localDataManager.getLocalVersion() == 0) {
                runBlocking(Dispatchers.IO) {
                    seedDatabaseFromAssets(this@CznApplication, database)
                }
                localDataManager.setLocalVersion(localDataManager.getAssetsVersion())
            }
        } catch (e: Exception) {
            Log.e("CznApp", "Block 1 failed: initial seed", e)
            try {
                val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
                crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nBlock 1 failed: ${e.message}\n${e.stackTraceToString()}\n---\n")
            } catch (_: Exception) {}
        }

        // Block 2: 安全网——数据库为空时重新导入（关键）
        try {
            runBlocking(Dispatchers.IO) {
                val charCount = database.characterDao().getCount()
                if (charCount == 0 && localDataManager.getLocalVersion() > 0) {
                    seedDatabaseFromAssets(this@CznApplication, database)
                    localDataManager.setLocalVersion(localDataManager.getAssetsVersion())
                }
                // 启动诊断：记录数据库状态
                try {
                    val diagLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
                    diagLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nBlock 2 OK: charCount=${charCount}, localVersion=${localDataManager.getLocalVersion()}\n---\n")
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("CznApp", "Block 2 failed: safety net seed", e)
            try {
                val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
                crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nBlock 2 failed: ${e.message}\n${e.stackTraceToString()}\n---\n")
            } catch (_: Exception) {}
        }

        // Block 3: 数据版本检查及迁移（允许失败）
        try {
            localDataManager.checkAndUpdateData(database, appScope)
        } catch (e: Exception) {
            Log.e("CznApp", "Block 3 failed: checkAndUpdateData", e)
            try {
                val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
                crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nBlock 3 failed: ${e.message}\n${e.stackTraceToString()}\n---\n")
            } catch (_: Exception) {}
        }

        // Block 4: 后台远程更新检查（允许失败）
        try {
            appScope.launch {
                remoteUpdateManager.startSilentBackgroundCheck()
            }
        } catch (e: Exception) {
            Log.e("CznApp", "Block 4 failed: remote check", e)
            try {
                val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
                crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nBlock 4 failed: ${e.message}\n${e.stackTraceToString()}\n---\n")
            } catch (_: Exception) {}
        }
    }
}
