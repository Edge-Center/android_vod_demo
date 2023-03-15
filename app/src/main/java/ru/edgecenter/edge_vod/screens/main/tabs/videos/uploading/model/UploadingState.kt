package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model

sealed class UploadingState {
    class Success(val videoName: String) : UploadingState()
    class Canceled(val videoName: String) : UploadingState()
    class Failure(val videoName: String) : UploadingState()
}
