package com.cznwiki.app.network

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Web content collector for fetching wiki data from the web.
 * 
 * Currently returns raw text/HTML - parsing is handled upstream.
 * Future: could integrate OCR for Gamekee's image-rendered pages.
 */
class WebCollector {
    companion object {
        private const val TAG = "WebCollector"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .build()
            chain.proceed(request)
        }
        .build()

    data class FetchResult(
        val url: String,
        val contentType: String = "",
        val contentLength: Long = 0,
        val body: String = "",
        val success: Boolean = false,
        val error: String = ""
    )

    /**
     * Fetch a single URL and return raw content.
     */
    suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                FetchResult(
                    url = url,
                    contentType = response.header("Content-Type", "") ?: "",
                    contentLength = body.length.toLong(),
                    body = body,
                    success = response.isSuccessful
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed: $url", e)
            FetchResult(url = url, success = false, error = e.message ?: "Unknown error")
        }
    }

    /**
     * Fetch multiple URLs in parallel.
     */
    suspend fun fetchAll(urls: List<String>): List<FetchResult> = coroutineScope {
        urls.map { url -> async { fetch(url) } }.awaitAll()
    }
}
