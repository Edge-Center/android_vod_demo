package ru.edgecenter.edge_vod.data.remote.video

import com.google.gson.annotations.SerializedName

data class UploadVideoResponse(
    @SerializedName("servers") val servers: List<ServerResponse>,
    @SerializedName("token") val uploadToken: String,
    @SerializedName("video") val uploadVideo: VideoItemResponse
)