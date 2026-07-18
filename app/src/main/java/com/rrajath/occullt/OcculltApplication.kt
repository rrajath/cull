package com.rrajath.occullt

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.rrajath.occullt.core.network.ImmichKeyGate
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

class OcculltApplication : Application() {

    companion object {
        lateinit var instance: OcculltApplication
            private set

        fun get(): OcculltApplication = instance

        var immichApiKey: String? = null
            private set

        var immichBaseUrl: String? = null
            private set

        fun setImmichCredentials(key: String?, baseUrl: String?) {
            immichApiKey = key
            immichBaseUrl = baseUrl
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val newRequest = if (ImmichKeyGate.shouldAttachKey(request.url, immichBaseUrl, immichApiKey)) {
                    request.newBuilder()
                        .addHeader("x-api-key", immichApiKey!!)
                        .build()
                } else {
                    request
                }
                chain.proceed(newRequest)
            })
            .build()

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components {
                    add(OkHttpNetworkFetcherFactory(okHttpClient))
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(this, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache").toOkioPath())
                        // photos stream from Immich; a roomy cache makes revisits instant
                        .maxSizeBytes(1024L * 1024 * 1024)
                        .build()
                }
                .build()
        }
    }
}
