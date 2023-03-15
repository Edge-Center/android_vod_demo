package ru.edgecenter.edge_vod.data.remote.account.auth

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("refresh") val refreshAccessToken: String,
    @SerializedName("access") val accessToken: String
)