package ru.edgecenter.edge_vod.screens.main.tabs.videos.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import ru.edgecenter.edge_vod.data.remote.RemoteAccessManager
import ru.edgecenter.edge_vod.data.remote.video.VideoItemResponse
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.RemoteVODsState
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem

class RemoteVODsViewModel(val app: Application) : AndroidViewModel(app) {

    private val compositeDisposable = CompositeDisposable()

    private val remoteVideos: MutableList<UiVideoItem> = ArrayList()
    private val _remoteVideosState = MutableLiveData<RemoteVODsState>(RemoteVODsState.Loading)
    val remoteVideosState: LiveData<RemoteVODsState> = _remoteVideosState

    init {
        getVideos()
    }

    fun getVideos() {
        remoteVideos.clear()
        _remoteVideosState.value = RemoteVODsState.Loading

        if (RemoteAccessManager.isAuth(app)) {
            getAccountVideos()
        } else {
            getDemoVideos()
        }
    }

    private fun getDemoVideos() {
        try {
            val inputStream = app.assets.open(DEMO_VIDEOS_FILE_NAME)
            val json = inputStream.bufferedReader().readText()

            val typeToken = object : TypeToken<List<VideoItemResponse>>(){}.type
            val demoVideos = Gson().fromJson<List<VideoItemResponse>>(json, typeToken)

            provideVideos(demoVideos)
        } catch (e: Exception) {
            _remoteVideosState.value = RemoteVODsState.Error
            e.printStackTrace()
        }
    }

    private fun getAccountVideos() {
        compositeDisposable.add(
            RemoteAccessManager.loadVideoItems(app)
                .subscribe({ videoItemResponses ->
                    provideVideos(videoItemResponses)
                }, {
                    if (it is HttpException && it.code() == 403) {
                        refreshToken()
                    } else {
                        _remoteVideosState.value = RemoteVODsState.Error
                    }
                })
        )
    }

    private fun provideVideos(videos: List<VideoItemResponse>) {
        if (videos.isNotEmpty()) {
            videos.forEach {
                remoteVideos.add(UiVideoItem.getInstance(it))
            }
            _remoteVideosState.value = RemoteVODsState.Success(remoteVideos)
        } else {
            _remoteVideosState.value = RemoteVODsState.Empty
        }
    }

    private fun refreshToken() {
        compositeDisposable.add(
            RemoteAccessManager.refreshToken(app)
                .subscribe({
                    RemoteAccessManager.updateTokens(app, it)
                    getVideos()
                }, {
                    if (it is HttpException && it.code() == 401) {
                        RemoteAccessManager.signOut(app)
                        _remoteVideosState.value = RemoteVODsState.AccessDenied
                    } else {
                        _remoteVideosState.value = RemoteVODsState.Error
                    }
                })
        )
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }

    companion object {
        private const val DEMO_VIDEOS_FILE_NAME = "demo_videos_for_mobile_apps.json"
    }
}