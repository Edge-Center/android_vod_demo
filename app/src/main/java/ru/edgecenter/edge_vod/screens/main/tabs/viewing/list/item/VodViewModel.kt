package ru.edgecenter.edge_vod.screens.main.tabs.viewing.list.item

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem
import ru.edgecenter.edge_vod.utils.VideoTrackSelector
import java.io.IOException

class VodViewModel(
    private val app: Application,
    private val videoData: UiVideoItem?
) : AndroidViewModel(app) {

    private val exoPlayerUtils = (app as ru.edgecenter.edge_vod.EdgeApp).exoPlayerUtils

    private var hlsDownloader: HlsDownloader? = null
    private val hlsDownloadHelper = run {
        val mediaItem = MediaItem.Builder()
            .setUri(videoData?.uri)
            .build()

        DownloadHelper.forMediaItem(
            app,
            mediaItem,
            DefaultRenderersFactory(app),
            exoPlayerUtils.upstreamDataSourceFactory
        )
    }
    private val bandwidthMeter = exoPlayerUtils.bandwidthMeter

    private var isReleasedDownloadHelper = true
    var isLoadingVideo: Boolean = false

    fun prepareMediaItem(mediaItemPrepared: (mediaItem: MediaItem) -> Unit) {
        if (isReleasedDownloadHelper) {
            val prepareCallback = object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper) {
                    val trackIndex = VideoTrackSelector.selectTrack(helper, bandwidthMeter)
                    val cacheStreamKeys = arrayListOf(
                        StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_VARIANT, trackIndex)
                    )
                    val preparedMediaItem = MediaItem.Builder()
                        .setUri(videoData?.uri)
                        .setStreamKeys(cacheStreamKeys)
                        .build()

                    mediaItemPrepared(preparedMediaItem)

                    hlsDownloadHelper.release()
                    isReleasedDownloadHelper = true
                }

                override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                    e.printStackTrace()

                    val preparedMediaItem = MediaItem.Builder()
                        .setUri(videoData?.uri)
                        .setStreamKeys(exoPlayerUtils.defaultCacheStreamKeys)
                        .build()

                    mediaItemPrepared(preparedMediaItem)

                    hlsDownloadHelper.release()
                    isReleasedDownloadHelper = true
                }
            }

            isReleasedDownloadHelper = false
            hlsDownloadHelper.prepare(prepareCallback)
        }
    }

    fun startPreCacheVideo(mediaItem: MediaItem) {
        isLoadingVideo = true

        hlsDownloader = HlsDownloader(
            mediaItem,
            exoPlayerUtils.cacheDataSourceFactory
        )
        viewModelScope.launch(Dispatchers.IO) {
            preCacheVideo()
        }
    }

    fun cancelPreCacheVideo() {
        hlsDownloader?.cancel()
        isLoadingVideo = false
        hlsDownloader = null
    }

    private suspend fun preCacheVideo() =
        withContext(Dispatchers.IO) {
            runCatching {
                hlsDownloader?.download { _, _, percentDownloaded ->
                    if (percentDownloaded >= PRE_CACHE_PERCENT) {
                        cancelPreCacheVideo()
                    }
                }
            }.onFailure {
                if (it is InterruptedException || it is CancellationException) return@onFailure
                it.printStackTrace()
            }.onSuccess {
                isLoadingVideo = false
            }
        }

    override fun onCleared() {
        hlsDownloader = null
        super.onCleared()
    }

    companion object {
        const val PRE_CACHE_PERCENT: Float = 30f
    }
}