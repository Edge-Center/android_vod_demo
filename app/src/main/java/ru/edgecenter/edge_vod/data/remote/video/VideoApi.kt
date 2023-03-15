package ru.edgecenter.edge_vod.data.remote.video

import io.reactivex.Single
import retrofit2.http.*

interface VideoApi {

    /**
     * @param page integer; Query parameter. Use it to list the paginated content
     * @param status integer; Query parameter. Use it to get videos filtered by their status. Possible values:
     * *     0 — empty (video has not been uploaded)
     * *     1 — pending (video is being processed)
     * *     2 — viewable (video is still being processed but can be viewed)
     * *     3 — ready (video is ready to be viewed)
     */
    @GET("./streaming/videos")
    fun getVideoItems(
        @Header("Authorization") accessToken: String,
        @Query("page") page: Int = 1,
        @Query("q[status_eq]") status: Int = 3
    ): Single<List<VideoItemResponse>>

    @POST("./streaming/videos")
    fun postVideo(
        @Header("Authorization") accessToken: String,
        @Body body: PostVideoRequestBody
    ): Single<VideoItemResponse>

    @GET("/streaming/videos/{video_id}/upload")
    fun getURLandTokenToUploadVideo(
        @Header("Authorization") accessToken: String,
        @Path("video_id") videoId: Int
    ): Single<UploadVideoResponse>
}