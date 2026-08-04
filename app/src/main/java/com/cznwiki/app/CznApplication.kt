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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CznApplication : Application(), ImageLoaderFactory {
    companion object {
        val initStatusFlow = MutableStateFlow("未开始")
        val initErrorFlow = MutableStateFlow<String?>(null)
    }

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

        // Step 1: localDataManager
        try {
            initStatusFlow.value = "Step1: localDataManager 初始化中..."
            val ver = localDataManager.getLocalVersion()
            initStatusFlow.value = "Step1 OK: localVersion=$ver"
        } catch (e: Exception) {
            initErrorFlow.value = "Step1 FAILED: ${e.message}\n${e.stackTraceToString().take(500)}"
            return
        }

        // Step 2: database instance
        try {
            initStatusFlow.value = "Step2: 数据库实例创建中..."
            val db = database
            initStatusFlow.value = "Step2 OK: database instance created"
        } catch (e: Exception) {
            initErrorFlow.value = "Step2 FAILED: ${e.message}\n${e.stackTraceToString().take(500)}"
            return
        }

        // Step 3: characterDao.getCount()
        try {
            initStatusFlow.value = "Step3: characterDao.getCount() 查询中..."
            val count = runBlocking(Dispatchers.IO) { database.characterDao().getCount() }
            initStatusFlow.value = "Step3 OK: charCount=$count"
        } catch (e: Exception) {
            initErrorFlow.value = "Step3 FAILED: ${e.message}\n${e.stackTraceToString().take(500)}"
            return
        }

        // Step 4: seed if empty
        try {
            val count = runBlocking(Dispatchers.IO) { database.characterDao().getCount() }
            if (count == 0) {
                initStatusFlow.value = "Step4: seedDatabaseFromAssets..."
                runBlocking(Dispatchers.IO) { seedDatabaseFromAssets(this@CznApplication, database) }
                initStatusFlow.value = "Step4 OK: seeded"
            } else {
                initStatusFlow.value = "Step4: skipped (already has data)"
            }
        } catch (e: Exception) {
            initErrorFlow.value = "Step4 FAILED: ${e.message}\n${e.stackTraceToString().take(500)}"
            return
        }

        // Final
        try {
            val finalCount = runBlocking(Dispatchers.IO) { database.characterDao().getCount() }
            initStatusFlow.value = "完成: charCount=$finalCount"
        } catch (e: Exception) {
            initErrorFlow.value = "Final FAILED: ${e.message}\n${e.stackTraceToString().take(500)}"
        }
    }
}
