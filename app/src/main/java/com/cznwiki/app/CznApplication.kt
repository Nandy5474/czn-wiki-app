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
    companion object {
        var initStatus: String = "未开始"
        var initError: String? = null
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
        // 跳过所有数据初始化——仅测试 UI 能否渲染
        Log.i("CznApp", ">>> Minimal init: no data loading, testing UI only")
    }
}
