package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading

import android.app.Application
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.*
import androidx.work.*
import io.reactivex.disposables.CompositeDisposable
import ru.edgecenter.edge_vod.data.remote.RemoteAccessManager
import ru.edgecenter.edge_vod.data.remote.UploadVideoWorker
import ru.edgecenter.edge_vod.data.remote.video.PostVideoRequestBody
import ru.edgecenter.edge_vod.data.remote.video.UploadVideoResponse
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UiUploadingVideo
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UploadingState
import ru.edgecenter.edge_vod.utils.extensions.add
import ru.edgecenter.edge_vod.utils.extensions.replaceAt

class UploadingVODsViewModel(val app: Application) : AndroidViewModel(app) {

    private val workManager = WorkManager.getInstance(app)
    private val compositeDisposable = CompositeDisposable()

    private val _uploadingVideoState = MutableLiveData<UploadingState?>(null)
    val uploadingVideoState: LiveData<UploadingState?> = _uploadingVideoState

    private val _uploadingVideos = MutableLiveData<MutableList<UiUploadingVideo>>(ArrayList())
    val uploadingVideos: LiveData<List<UiUploadingVideo>> = _uploadingVideos.map {
        it.toList()
    }

    fun uploadVideo(localVideoUri: Uri) {
        val requestBody = getPostVideoRequestBody(localVideoUri)

        if (requestBody != null) {
            compositeDisposable.add(
                RemoteAccessManager.postVideo(app, requestBody)
                    .subscribe({
                        getUrlAndTokenToUploadVideo(
                            it.id,
                            requestBody.videoName,
                            localVideoUri.toString()
                        )
                    }, {
                        _uploadingVideoState.value = UploadingState.Failure(requestBody.videoName)
                    })
            )
        } else {
            _uploadingVideoState.value = UploadingState.Failure("")
        }
    }

    fun resumeUpload(video: UiUploadingVideo) {
        val videoData = Data.Builder()
            .putString(
                UploadVideoWorker.UPLOAD_VIDEO_URL,
                "https://${video.remoteData.servers[0].hostname}/upload/"
            )
            .putString(UploadVideoWorker.VIDEO_NAME, video.remoteData.uploadVideo.name)
            .putInt(UploadVideoWorker.VIDEO_ID, video.remoteData.uploadVideo.id)
            .putString(UploadVideoWorker.VIDEO_TOKEN, video.remoteData.uploadToken)
            .putString(UploadVideoWorker.VIDEO_LOCAL_URI, video.localUri)
            .putInt(UploadVideoWorker.CLIENT_ID, video.remoteData.uploadVideo.clientId)
            .build()

        val uploadVideoTask = getUploadTask(videoData)
        workManager.enqueueUniqueWork(
            video.remoteData.uploadVideo.id.toString(),
            ExistingWorkPolicy.REPLACE,
            uploadVideoTask
        )

        val index = _uploadingVideos.value?.indexOf(video)
        video.state = workManager.getWorkInfoByIdLiveData(uploadVideoTask.id)
        if (index == -1) {
            _uploadingVideos.add(video)
        } else {
            index?.let {
                _uploadingVideos.replaceAt(it, video)
            }
        }

        listenUploadVideoWorkerState(video)
    }

    fun cancelUpload(videoId: Int) {
        workManager.cancelUniqueWork(videoId.toString())
    }

    private fun getPostVideoRequestBody(videoUri: Uri): PostVideoRequestBody? {
        app.contentResolver.query(
            videoUri,
            null,
            null,
            null,
            null
        )?.use { cursor: Cursor ->
            return PostVideoRequestBody.getInstance(cursor)
        }
        return null
    }

    private fun getUrlAndTokenToUploadVideo(videoId: Int, videoName: String, videoUri: String) {
        compositeDisposable.add(
            RemoteAccessManager.getUrlAndTokenToUploadVideo(app, videoId)
                .subscribe({
                    startUploadVideoWorker(it, videoUri)
                }, {
                    _uploadingVideoState.value = UploadingState.Failure(videoName)
                })
        )
    }

    private fun startUploadVideoWorker(
        uploadVideoResponse: UploadVideoResponse,
        videoLocalUri: String
    ) {
        val uploadingVideo = UiUploadingVideo(
            uploadVideoResponse.uploadVideo.name,
            uploadVideoResponse,
            videoLocalUri
        )
        resumeUpload(uploadingVideo)
    }

    private fun getUploadTask(videoData: Data): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequest.Builder(UploadVideoWorker::class.java)
            .setConstraints(constraints)
            .setInputData(videoData)
            .build()
    }

    private fun listenUploadVideoWorkerState(uploadingVideo: UiUploadingVideo) {
        uploadingVideo.apply {
            state?.observeForever(object : Observer<WorkInfo> {
                override fun onChanged(workInfo: WorkInfo) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _uploadingVideoState.value = UploadingState.Success(videoName)
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uploadingVideoState.value = UploadingState.Canceled(videoName)
                        }
                        WorkInfo.State.FAILED -> {
                            _uploadingVideoState.value = UploadingState.Failure(videoName)
                        }
                        else -> {}
                    }
                    if (workInfo.state.isFinished) {
                        _uploadingVideoState.value = null
                        state?.removeObserver(this)
                    }
                }
            })
        }
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}