package com.cznwiki.app

import android.app.Application
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

        // Global crash capture
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            val crashLog = java.io.File(this@CznApplication.getExternalFilesDir(null), "crash_log.txt")
            crashLog.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\nThread: ${thread.name}\n${e.stackTraceToString()}\n---\n")
        }

        Log.i("CznApp", ">>> APP START")

        // Block 1: First-time seed if no local version
        try {
            val localVer = localDataManager.getLocalVersion()
            if (localVer == 0) {
                Log.i("CznApp", "First run (localVersion=0), seeding from assets...")
                runBlocking(Dispatchers.IO) {
                    seedDatabaseFromAssets(this@CznApplication, database)
                    localDataManager.setLocalVersion(1)
                }
                Log.i("CznApp", "Block1 OK: first-time seed complete")
            } else {
                Log.i("CznApp", "Block1 OK: localVersion=$localVer, skip seed")
            }
        } catch (e: Exception) {
            Log.e("CznApp", "Block1 FAILED: ${e.message}", e)
        }

        // Block 2: Safety net — if DB still empty, re-seed
        try {
            val count = runBlocking(Dispatchers.IO) { database.characterDao().getCount() }
            if (count == 0) {
                Log.w("CznApp", "Block2: charCount=0, safety net re-seed...")
                runBlocking(Dispatchers.IO) {
                    seedDatabaseFromAssets(this@CznApplication, database)
                }
                Log.i("CznApp", "Block2 OK: safety net seed complete")
            } else {
                Log.i("CznApp", "Block2 OK: charCount=$count")
            }
        } catch (e: Exception) {
            Log.e("CznApp", "Block2 FAILED: ${e.message}", e)
        }

        // Block 3: Check and update from OTA data
        try {
            localDataManager.checkAndUpdateData(database, appScope)
            Log.i("CznApp", "Block3 OK: checkAndUpdateData finished")
        } catch (e: Exception) {
            Log.e("CznApp", "Block3 FAILED: ${e.message}", e)
        }

        // Block 4: Remote background check
        try {
            appScope.launch {
                remoteUpdateManager.startSilentBackgroundCheck()
            }
            Log.i("CznApp", "Block4 OK: remote check launched")
        } catch (e: Exception) {
            Log.e("CznApp", "Block4 FAILED: ${e.message}", e)
        }
    }
}
