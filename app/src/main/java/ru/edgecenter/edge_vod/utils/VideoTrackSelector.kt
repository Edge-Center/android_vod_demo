package ru.edgecenter.edge_vod.utils

import android.util.Log
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
import com.google.android.exoplayer2.upstream.BandwidthMeter

object VideoTrackSelector {

    /**
     * Selects the appropriate track based on the bandwidth.
     * Returns the index of the selected track
     */
    fun selectTrack(helper: DownloadHelper, bandwidthMeter: BandwidthMeter): Int {
        val trackResolution = pickResolution(bandwidthMeter)

        val trackGroup = helper.getTrackGroups(0)
            .get(HlsMultivariantPlaylist.GROUP_INDEX_VARIANT)

        return if (trackResolution != null) {
            getMatchingTrackIndex(trackResolution, trackGroup)
        } else {
            getMinResolutionTrackIndex(trackGroup)
        }

    }

    private fun pickResolution(bandwidthMeter: BandwidthMeter): VideoResolution? {
        val networkSpeedInMbs = bandwidthMeter.bitrateEstimate / 1024 / 1024
        Log.d("tracccks", "networkSpeedInMbs: $networkSpeedInMbs")

        return when (networkSpeedInMbs) {
            in 20..1000 -> VideoResolution.HIGH
            in 12 until 20 -> VideoResolution.MIDDLE
            in 5 until 12 -> VideoResolution.LOW
            in 2 until 5 -> VideoResolution.LOWEST
            else -> null
        }
    }

    private fun getMatchingTrackIndex(
        trackResolution: VideoResolution,
        trackGroup: TrackGroup
    ): Int {
        var matchingTrackIndex = -1

        for (trackIndex in 0 until trackGroup.length) {
            val width = trackGroup.getFormat(trackIndex).width
            val height = trackGroup.getFormat(trackIndex).height
            val isVerticalVideo = width < height

            if (isVerticalVideo) {
                if (width in trackResolution.range) {
                    matchingTrackIndex = trackIndex
                    break
                }
            } else {
                if (height in trackResolution.range) {
                    matchingTrackIndex = trackIndex
                    break
                }
            }
        }

        return if (matchingTrackIndex >= 0) {
            matchingTrackIndex
        }
        else {
            getMinResolutionTrackIndex(trackGroup)
        }
    }

    private fun getMinResolutionTrackIndex(trackGroup: TrackGroup): Int {
        var minResolution = Int.MAX_VALUE
        var minResolutionIndex = -1

        for (trackIndex in 0 until trackGroup.length) {
            val width = trackGroup.getFormat(trackIndex).width
            val height = trackGroup.getFormat(trackIndex).height
            val isVerticalFormat = width < height

            if (isVerticalFormat) {
                if (width < minResolution) {
                    minResolution = width
                    minResolutionIndex = trackIndex
                }
            } else {
                if (height < minResolution) {
                    minResolution = height
                    minResolutionIndex = trackIndex
                }
            }
        }

        return minResolutionIndex
    }

    private enum class VideoResolution(val range: IntRange) {
        LOWEST(300 until 400),
        LOW(400..500),
        MIDDLE(700..800),
        HIGH(1000..1100)
    }
}