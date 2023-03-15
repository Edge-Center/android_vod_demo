package ru.edgecenter.edge_vod.data.remote.account.refresh_token

import com.google.gson.annotations.SerializedName

class RefreshRequestBody(
    @SerializedName("refresh") val refreshAccessToken: String
)