package com.cznwiki.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.disk.DiskCache
import com.cznwiki.app.data.database.AppDatabase
import com.cznwiki.app.data.database.seedDatabaseFromAssets

class CznApplication : Application(), ImageLoaderFactory {
    val database by lazy { AppDatabase.getInstance(this) }

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
            .crossfade(300)
            .allowHardware(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // Seed data on first launch
        seedDatabaseFromAssets(this, database)
    }
}
