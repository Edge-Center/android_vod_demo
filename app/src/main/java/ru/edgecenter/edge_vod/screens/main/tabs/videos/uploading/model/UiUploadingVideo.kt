package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model

import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import ru.edgecenter.edge_vod.data.remote.video.UploadVideoResponse

data class UiUploadingVideo(
    val videoName: String,
    val remoteData: UploadVideoResponse,
    val localUri: String,
    var state: LiveData<WorkInfo>? = null
)