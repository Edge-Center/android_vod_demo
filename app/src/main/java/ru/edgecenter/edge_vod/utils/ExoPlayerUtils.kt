package ru.edgecenter.edge_vod.utils

import android.app.Application
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import edge_vod.R
import java.io.File

class ExoPlayerUtils(private val app: Application) {

    private val simpleCache by lazy {
        return@lazy simpleCacheInstance ?: run {
            val exoPlayerCacheDir = File("${app.cacheDir.absolutePath}/exo")
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_IN_BYTES)
            val dataBaseProvider = StandaloneDatabaseProvider(app)

            SimpleCache(exoPlayerCacheDir, cacheEvictor, dataBaseProvider).also {
                simpleCacheInstance = it
            }
        }
    }

    val upstreamDataSourceFactory = DefaultDataSource.Factory(
        app,
        DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(app, app.getString(R.string.app_name)))
    )

    val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)

    val bandwidthMeter = DefaultBandwidthMeter.Builder(app)
        .build()

    val defaultCacheStreamKeys = arrayListOf(
        StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_VARIANT, 0),
    )

    companion object {
        private var simpleCacheInstance: SimpleCache? = null
        private const val CACHE_SIZE_IN_BYTES = 90 * 1024 * 1024L
    }
}