package ru.edgecenter.edge_vod.data.remote.video

import com.google.gson.annotations.SerializedName

data class ServerResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("role") val role: String,
    @SerializedName("ip") val ip: String?,
    @SerializedName("hostname") val hostname: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("ssl") val ssl: Boolean
)