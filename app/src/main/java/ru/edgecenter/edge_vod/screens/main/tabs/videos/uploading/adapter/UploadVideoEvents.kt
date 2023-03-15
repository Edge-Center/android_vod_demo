package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.adapter

import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UiUploadingVideo


interface UploadVideoEvents {
    fun onWatchVideo(video: UiUploadingVideo)
    fun onCancelUpload(video: UiUploadingVideo)
    fun onResumeUpload(video: UiUploadingVideo)
}