package com.cznwiki.app.coil

import android.content.Context
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import coil.decode.DataSource
import coil.decode.ImageSource
import okio.buffer
import okio.source

class AssetUriFetcher(
    private val context: Context,
    private val data: String,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val path = data.removePrefix(ASSET_PREFIX)
        val inputStream = context.assets.open(path)
        val source = inputStream.source().buffer()
        return SourceResult(
            source = ImageSource(source = source, context = context),
            mimeType = if (path.endsWith(".webp")) "image/webp" else null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.startsWith(ASSET_PREFIX)) return null
            return AssetUriFetcher(context, data)
        }
    }

    companion object {
        const val ASSET_PREFIX = "file:///android_asset/"
    }
}
