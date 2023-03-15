package ru.edgecenter.edge_vod.data.remote.video

import android.database.Cursor
import android.provider.MediaStore
import com.google.gson.annotations.SerializedName

class PostVideoRequestBody(
    @SerializedName("name") val videoName: String,
    @SerializedName("description") val videoDescription: String? = null,
    @SerializedName("duration") val videoDuration: Long? = null,
    @SerializedName("share_url") val videoShareUrl: String? = null,
    @SerializedName("custom_iframe_url") val videoCustomIframeUrl: String? = null,
    @SerializedName("origin_size") val videoSizeInByte: Long? = null,
    @SerializedName("origin_height") val videoHeight: Int?,
    @SerializedName("origin_width") val videoWidth: Int?,
    @SerializedName("screenshot_id") val videoScreenshotId: Int = 0,
    @SerializedName("ad_id") val videoAdId: Int? = null,
    @SerializedName("projection") val videoProjection: String = "regular",
    @SerializedName("client_user_id") val clientUserId: Int? = null,
    @SerializedName("stream_id") val videoStreamId: Int? = null,
    @SerializedName("poster") val videoPoster: String? = null
) {
    companion object {
        fun getInstance(videoResCursor: Cursor): PostVideoRequestBody {
            videoResCursor.moveToFirst()

            with(videoResCursor) {
                return PostVideoRequestBody(
                    videoName = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.TITLE)),
                    videoDescription = getString(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DESCRIPTION)),
                    videoDuration = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)),
                    videoSizeInByte = getLong(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.SIZE)),
                    videoHeight = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT)),
                    videoWidth = getInt(getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH)),
                )
            }
        }
    }
}