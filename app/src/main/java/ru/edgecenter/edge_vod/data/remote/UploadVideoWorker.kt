package ru.edgecenter.edge_vod.data.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import edge_vod.R
import io.tus.android.client.TusAndroidUpload
import io.tus.android.client.TusPreferencesURLStore
import io.tus.java.client.ProtocolException
import io.tus.java.client.TusClient
import io.tus.java.client.TusExecutor
import java.io.IOException
import java.net.URL

class UploadVideoWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val notificationId = NOTIFICATION_ID++

        return try {
            val tusClient = getTusClient()
            val videoUpload = getVideoUpload()

            uploadVideo(tusClient, videoUpload) { uploadedBytes: Long ->
                setForegroundAsync(
                    createForegroundInfo(
                        notificationId,
                        videoUpload.size.toInt(),
                        uploadedBytes.toInt()
                    )
                )

                val uploadedPercent = (uploadedBytes * 100 / videoUpload.size).toInt()
                setProgressAsync(
                    workDataOf(
                        UPLOADED_PERCENT to uploadedPercent,
                    )
                )
            }
            Result.success()

        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            Result.failure()
        }
    }

    private fun uploadVideo(
        tusClient: TusClient,
        videoUpload: TusAndroidUpload,
        updateProgress: (uploadedBytes: Long) -> Unit
    ) {
        val executor = object : TusExecutor() {
            override fun makeAttempt() {
                try {
                    val uploader = tusClient.resumeOrCreateUpload(videoUpload).apply {
                        chunkSize = uploadedChunkSizeInBytes
                    }

                    val progressChunkSize = videoUpload.size * stepDisplayedProgressInPercents / 100
                    var displayedOffset = progressChunkSize

                    do {
                        if (uploader.offset > displayedOffset) {
                            displayedOffset += progressChunkSize
                            updateProgress(uploader.offset)
                        }
                    } while (!isStopped && (uploader.uploadChunk() > -1))

                    uploader.finish()
                } catch (e: ProtocolException) {
                    throw ProtocolException(e.message)
                } catch (e: IOException) {
                    throw IOException(e.message)
                }
            }
        }
        executor.makeAttempts()
    }

    private fun getTusClient(): TusClient {
        val uploadUrl = inputData.getString(UPLOAD_VIDEO_URL) ?: ""
        val urlStore = TusPreferencesURLStore(
            context.getSharedPreferences("TUS", 0)
        )

        return TusClient().apply {
            uploadCreationURL = URL(uploadUrl)
            enableResuming(urlStore)
        }
    }

    private fun getVideoUpload(): TusAndroidUpload {
        val videoName = inputData.getString(VIDEO_NAME) ?: ""
        val clientId = inputData.getInt(CLIENT_ID, 0)
        val videoId = inputData.getInt(VIDEO_ID, 0)
        val videoLocalUri = Uri.parse(
            inputData.getString(VIDEO_LOCAL_URI)
        )
        val token = inputData.getString(VIDEO_TOKEN) ?: ""

        return TusAndroidUpload(videoLocalUri, context).apply {
            metadata = mapOf(
                "filename" to videoName,
                "client_id" to clientId.toString(),
                "video_id" to videoId.toString(),
                "token" to token
            )
        }
    }

    private fun createForegroundInfo(
        notificationId: Int,
        maxProgress: Int,
        currentProgress: Int
    ) = ForegroundInfo(
        notificationId,
        getNotification(maxProgress, currentProgress)
    )

    private fun getNotification(maxProgress: Int, currentProgress: Int): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val cancelIntent = WorkManager.getInstance(context)
            .createCancelPendingIntent(id)

        val contentTitle = inputData.getString(VIDEO_NAME)
        val contentText = context.getString(R.string.upload_video)

        return NotificationCompat.Builder(context, UPLOAD_VIDEO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_edge_app)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(maxProgress, currentProgress, false)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            UPLOAD_VIDEO_CHANNEL_ID,
            UPLOAD_VIDEO_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "channel for upload video"
        }

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .apply {
                createNotificationChannel(notificationChannel)
            }
    }

    companion object {
        private var NOTIFICATION_ID = 101
        private const val UPLOAD_VIDEO_CHANNEL_ID = "uploadVideoChannelId"
        private const val UPLOAD_VIDEO_CHANNEL_NAME = "uploadVideoChannelName"

        private const val uploadedChunkSizeInBytes = 50 * 1024
        private const val stepDisplayedProgressInPercents = 5

        const val UPLOAD_VIDEO_URL = "uploadVideoUrl"
        const val VIDEO_NAME = "name"
        const val CLIENT_ID = "clientId"
        const val VIDEO_TOKEN = "videoToken"
        const val VIDEO_ID = "id"
        const val VIDEO_LOCAL_URI = "videoLocalUri"
        const val UPLOADED_PERCENT = "uploadedPercent"
    }
}