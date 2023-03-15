package ru.edgecenter.edge_vod.data.remote.video

import com.google.gson.annotations.SerializedName

data class VideoItemResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("client_id") val clientId: Int,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("slug") val endPartURL: String,
    @SerializedName("status") val videoStatus: String,
    @SerializedName("share_url") val videoShareURL: String?,
    @SerializedName("custom_iframe_url") val customIframeURL: String?,
    @SerializedName("origin_filename") val originVideoName: String?,
    @SerializedName("origin_size") val originVideoSize: Long?,
    @SerializedName("origin_storage") val originVideoStorage: Any?,
    @SerializedName("origin_host") val originVideoHost: String?,
    @SerializedName("origin_resource") val originVideoResource: String?,
    @SerializedName("origin_audio_channels") val originAudioChannels: Int?,
    @SerializedName("origin_height") val originHeightVideo: Int,
    @SerializedName("origin_width") val originWidthVideo: Int,
    @SerializedName("screenshots") val screenshots: List<String?>?,
    @SerializedName("screenshot_id") val screenshotId: Int,
    @SerializedName("ad_id") val adId: Int?,
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("client_user_id") val clientUserId: Int?,
    @SerializedName("recording_started_at") val recordingStartedAt: Any?,
    @SerializedName("projection") val videoProjection: String,
    @SerializedName("player_id") val playerId: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("encryption") val encryption: String?,
    @SerializedName("hls_url") val hlsURL: String,
    @SerializedName("poster_thumb") val posterThumb: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("screenshot") val screenshot: String?,
    @SerializedName("views") val amountViews: Int,
    @SerializedName("folders") val folders: List<Any>
)