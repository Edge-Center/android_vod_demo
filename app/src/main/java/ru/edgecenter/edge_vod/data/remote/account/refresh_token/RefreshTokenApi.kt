package ru.edgecenter.edge_vod.data.remote.account.refresh_token

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import ru.edgecenter.edge_vod.data.remote.account.auth.AuthResponse

interface RefreshTokenApi {
    @POST("./iam/auth/jwt/refresh")
    fun refreshToken(@Body body: RefreshRequestBody): Single<AuthResponse>
}