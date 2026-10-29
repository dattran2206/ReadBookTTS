package com.vietreader.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as AndroidPlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.vietreader.tts.MainActivity
import com.vietreader.tts.R
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.domain.model.CoverGradient
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.domain.repository.BookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 * Foreground Service managing background TTS audio playback, WakeLock, AudioFocus,
 * Bluetooth / Lock Screen [MediaSession] controls, and Android MediaStyle Notification.
 */
class PlaybackForegroundService : Service() {

    companion object {
        private const val TAG = "PlaybackFgService"

        const val CHANNEL_ID = "wordwave_media_playback_channel"
        private const val CHANNEL_NAME = "Phát sách nói Wordwave"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_OR_UPDATE = "com.vietreader.tts.action.START_OR_UPDATE"
        const val ACTION_PLAY = "com.vietreader.tts.action.PLAY"
        const val ACTION_PAUSE = "com.vietreader.tts.action.PAUSE"
        const val ACTION_PLAY_PAUSE = "com.vietreader.tts.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.vietreader.tts.action.NEXT"
        const val ACTION_PREVIOUS = "com.vietreader.tts.action.PREVIOUS"
        const val ACTION_STOP = "com.vietreader.tts.action.STOP"

        const val EXTRA_OPEN_READER = "extra_open_reader_from_notification"
        const val EXTRA_BOOK_ID = "extra_book_id_from_notification"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startOrUpdate(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, PlaybackForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            runCatching {
                ContextCompat.startForegroundService(appContext, intent)
            }.onFailure { e ->
                Log.w(TAG, "Could not start PlaybackForegroundService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.stopService(Intent(appContext, PlaybackForegroundService::class.java))
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackForegroundService = this@PlaybackForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository: BookRepository by lazy { BookRepositoryImpl.instance }

    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var resumeOnFocusGain: Boolean = false

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForegroundPromoted: Boolean = false
    private var isNoisyReceiverRegistered: Boolean = false
    private var idleShutdownJob: Job? = null

    private var cachedCoverPath: String? = null
    private var cachedGradient: CoverGradient? = null
    private var cachedCoverBitmap: Bitmap? = null

    private var lastNotifiedBookId: String = ""
    private var lastNotifiedChapter: String = ""
    private var lastNotifiedSentenceIdx: Int = -1
    private var lastNotifiedPlaying: Boolean? = null
    private var lastNotifiedSecond: Int = -1
    private var lastNotifiedSleepMinute: Int = -1

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.i(TAG, "Audio becoming noisy (headset disconnected) -> pausing playback")
                resumeOnFocusGain = false
                repository.pausePlayback()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.i(TAG, "AudioFocus lost permanently -> pausing playback")
                hasAudioFocus = false
                resumeOnFocusGain = false
                repository.pausePlayback()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val currentlyPlaying = repository.playbackState.value?.isPlaying == true
                if (currentlyPlaying) {
                    Log.i(TAG, "AudioFocus lost transiently -> pausing and marking resumeOnFocusGain")
                    resumeOnFocusGain = true
                    repository.pausePlayback()
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (resumeOnFocusGain) {
                    Log.i(TAG, "AudioFocus regained -> resuming playback")
                    resumeOnFocusGain = false
                    repository.resumePlayback()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        repository.initialize(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        initMediaSession()
        observePlaybackState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                resumeOnFocusGain = false
                if (requestAudioFocus()) {
                    repository.resumePlayback()
                }
            }

            ACTION_PAUSE -> {
                resumeOnFocusGain = false
                repository.pausePlayback()
            }

            ACTION_PLAY_PAUSE -> {
                resumeOnFocusGain = false
                val isPlaying = repository.playbackState.value?.isPlaying == true
                if (isPlaying) {
                    repository.pausePlayback()
                } else if (requestAudioFocus()) {
                    repository.resumePlayback()
                }
            }

            ACTION_NEXT -> {
                repository.skipToNextSentence()
            }

            ACTION_PREVIOUS -> {
                repository.skipToPreviousSentence()
            }

            ACTION_STOP -> {
                handleStopCommand()
                return START_NOT_STICKY
            }

            else -> {
                val state = repository.playbackState.value ?: PlaybackState.EMPTY
                val sleepRemSec = repository.sleepTimerRemainingSeconds.value
                val sleepMin = repository.sleepTimerMinutes.value
                if (state.isPlaying) {
                    requestAudioFocus()
                }
                promoteOrUpdateNotification(state, sleepMin, sleepRemSec, forceUpdate = true)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isPlaying = repository.playbackState.value?.isPlaying == true
        if (!isPlaying) {
            handleStopCommand()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isServiceRunning = false
        idleShutdownJob?.cancel()
        idleShutdownJob = null
        unregisterNoisyReceiver()
        abandonAudioFocus()
        releaseWakeLock()
        repository.releaseAiVoiceEngine()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            combine(
                repository.playbackState,
                repository.sleepTimerMinutes,
                repository.sleepTimerRemainingSeconds
            ) { playback, sleepMinutes, sleepRemainingSec ->
                Triple(playback, sleepMinutes, sleepRemainingSec)
            }.collect { (playback, sleepMinutes, sleepRemainingSec) ->
                if (playback == null || playback.bookId.isBlank()) {
                    idleShutdownJob?.cancel()
                    idleShutdownJob = null
                    if (isForegroundPromoted) {
                        demoteForeground(removeNotification = true)
                    }
                    releaseWakeLock()
                    unregisterNoisyReceiver()
                    return@collect
                }

                if (playback.isPlaying) {
                    idleShutdownJob?.cancel()
                    idleShutdownJob = null
                    requestAudioFocus()
                    acquireWakeLock()
                    registerNoisyReceiver()
                } else {
                    releaseWakeLock()
                    unregisterNoisyReceiver()
                    scheduleIdleShutdownIfPaused()
                }

                updateMediaSessionState(playback)

                val currentSleepMinRounded = if (sleepMinutes > 0) (sleepRemainingSec + 59) / 60 else 0
                val majorChange = playback.bookId != lastNotifiedBookId ||
                    playback.chapterTitle != lastNotifiedChapter ||
                    playback.activeSentenceIndex != lastNotifiedSentenceIdx ||
                    playback.isPlaying != lastNotifiedPlaying ||
                    currentSleepMinRounded != lastNotifiedSleepMinute
                val timeTickChange = abs(playback.currentSeconds - lastNotifiedSecond) >= 5

                if (majorChange || timeTickChange) {
                    promoteOrUpdateNotification(
                        state = playback,
                        sleepMinutes = sleepMinutes,
                        sleepRemainingSec = sleepRemainingSec,
                        forceUpdate = majorChange
                    )
                }
            }
        }
    }

    private fun initMediaSession() {
        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_READER, true)
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            100,
            sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession(this, "WordwaveMediaSession").apply {
            setSessionActivity(sessionActivityPendingIntent)
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumeOnFocusGain = false
                    if (requestAudioFocus()) {
                        repository.resumePlayback()
                    }
                }

                override fun onPause() {
                    resumeOnFocusGain = false
                    repository.pausePlayback()
                }

                override fun onSkipToNext() {
                    repository.skipToNextSentence()
                }

                override fun onSkipToPrevious() {
                    repository.skipToPreviousSentence()
                }

                override fun onFastForward() {
                    repository.seekByDeltaSeconds(15)
                }

                override fun onRewind() {
                    repository.seekByDeltaSeconds(-15)
                }

                override fun onSeekTo(pos: Long) {
                    repository.seekToSeconds((pos / 1000L).toInt())
                }

                override fun onStop() {
                    handleStopCommand()
                }

                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    @Suppress("DEPRECATION")
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                onPlay()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                onPause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                val isPlaying = repository.playbackState.value?.isPlaying == true
                                if (isPlaying) onPause() else onPlay()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                onSkipToNext()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                onSkipToPrevious()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                onFastForward()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                onRewind()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                onStop()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState(state: PlaybackState) {
        val session = mediaSession ?: return
        val coverBitmap = resolveCoverBitmap(state)

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, state.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "${state.chapterShortLabel} • ${state.author}")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, state.chapterTitle)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, state.title)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, state.chapterTitle)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, state.author)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, (state.totalSeconds.coerceAtLeast(1)) * 1000L)
            .apply {
                if (coverBitmap != null) {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap)
                    putBitmap(MediaMetadata.METADATA_KEY_ART, coverBitmap)
                }
            }
            .build()
        session.setMetadata(metadata)

        val playbackActions = AndroidPlaybackState.ACTION_PLAY or
            AndroidPlaybackState.ACTION_PAUSE or
            AndroidPlaybackState.ACTION_PLAY_PAUSE or
            AndroidPlaybackState.ACTION_SKIP_TO_NEXT or
            AndroidPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            AndroidPlaybackState.ACTION_SEEK_TO or
            AndroidPlaybackState.ACTION_FAST_FORWARD or
            AndroidPlaybackState.ACTION_REWIND or
            AndroidPlaybackState.ACTION_STOP

        val androidState = if (state.isPlaying) {
            AndroidPlaybackState.STATE_PLAYING
        } else {
            AndroidPlaybackState.STATE_PAUSED
        }

        val sessionPlaybackState = AndroidPlaybackState.Builder()
            .setActions(playbackActions)
            .setState(
                androidState,
                state.currentSeconds * 1000L,
                if (state.isPlaying) state.playbackSpeed else 0f,
                SystemClock.elapsedRealtime()
            )
            .build()

        session.setPlaybackState(sessionPlaybackState)
    }

    private fun promoteOrUpdateNotification(
        state: PlaybackState,
        sleepMinutes: Int,
        sleepRemainingSec: Int,
        forceUpdate: Boolean
    ) {
        lastNotifiedBookId = state.bookId
        lastNotifiedChapter = state.chapterTitle
        lastNotifiedSentenceIdx = state.activeSentenceIndex
        lastNotifiedPlaying = state.isPlaying
        lastNotifiedSecond = state.currentSeconds
        lastNotifiedSleepMinute = if (sleepMinutes > 0) (sleepRemainingSec + 59) / 60 else 0

        val notification = buildMediaNotification(state, sleepMinutes, sleepRemainingSec)

        if (!isForegroundPromoted) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForegroundPromoted = true
                // If startForegroundService() was called right before playback paused, satisfy Android 14's
                // mandatory startForeground() contract first and then immediately detach foreground status
                if (!state.isPlaying) {
                    demoteForeground(removeNotification = false)
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to promote foreground service: ${e.message}")
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } else if (isForegroundPromoted || forceUpdate || state.bookId.isNotBlank()) {
            if (!state.isPlaying && isForegroundPromoted) {
                // Keep notification visible while paused so user can resume from Lock Screen
                demoteForeground(removeNotification = false)
            }
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildMediaNotification(
        state: PlaybackState,
        sleepMinutes: Int,
        sleepRemainingSec: Int
    ): Notification {
        val openReaderIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_READER, true)
            putExtra(EXTRA_BOOK_ID, state.bookId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            101,
            openReaderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_prev),
            "Câu trước",
            buildServicePendingIntent(ACTION_PREVIOUS, 201)
        ).build()

        val playPauseAction = if (state.isPlaying) {
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_notif_pause),
                "Tạm dừng",
                buildServicePendingIntent(ACTION_PAUSE, 202)
            ).build()
        } else {
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_notif_play),
                "Phát tiếp",
                buildServicePendingIntent(ACTION_PLAY, 203)
            ).build()
        }

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_next),
            "Câu tiếp",
            buildServicePendingIntent(ACTION_NEXT, 204)
        ).build()

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_stop),
            "Dừng",
            buildServicePendingIntent(ACTION_STOP, 205)
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val subText = if (sleepMinutes > 0 && sleepRemainingSec > 0) {
            val mins = (sleepRemainingSec + 59) / 60
            "Hẹn giờ ngủ: ${mins}p • ${state.voiceName}"
        } else {
            "Đoạn ${state.activeSentenceIndex + 1} • ${state.voiceName}"
        }

        val coverBitmap = resolveCoverBitmap(state)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_wordwave)
            .setContentTitle(state.title.ifBlank { "Wordwave" })
            .setContentText(state.chapterTitle.ifBlank { state.author })
            .setSubText(subText)
            .setLargeIcon(coverBitmap)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(buildServicePendingIntent(ACTION_STOP, 206))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(state.isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setStyle(mediaStyle)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .build()
    }

    private fun buildServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleStopCommand() {
        idleShutdownJob?.cancel()
        idleShutdownJob = null
        resumeOnFocusGain = false
        repository.stopPlayback()
        repository.releaseAiVoiceEngine()
        abandonAudioFocus()
        releaseWakeLock()
        unregisterNoisyReceiver()
        demoteForeground(removeNotification = true)
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    /**
     * Automatically releases native AI TTS memory and stops the service after 15 minutes of
     * paused inactivity (e.g. after Sleep Timer expires overnight) to prevent background battery drain.
     */
    private fun scheduleIdleShutdownIfPaused() {
        if (idleShutdownJob?.isActive == true) return
        idleShutdownJob = serviceScope.launch {
            delay(15 * 60 * 1000L)
            if (repository.playbackState.value?.isPlaying != true) {
                Log.i(TAG, "Service paused and idle for 15 minutes -> releasing native resources and stopping self")
                handleStopCommand()
            }
        }
    }

    private fun demoteForeground(removeNotification: Boolean) {
        if (!isForegroundPromoted) return
        runCatching {
            val flags = if (removeNotification) {
                STOP_FOREGROUND_REMOVE
            } else {
                STOP_FOREGROUND_DETACH
            }
            stopForeground(flags)
        }
        isForegroundPromoted = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Điều khiển phát sách nói chạy nền và màn hình khóa"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager?.createNotificationChannel(channel)
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = audioManager ?: return true
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
        audioFocusRequest = req
        val res = am.requestAudioFocus(req)
        hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        hasAudioFocus = false
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val lock = wakeLock ?: pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Wordwave::TtsPlaybackWakeLock"
            ).apply {
                setReferenceCounted(false)
            }.also { wakeLock = it }
            // 4 hours safety timeout so WakeLock never leaks indefinitely
            lock.acquire(4 * 60 * 60 * 1000L)
        }.onFailure { e ->
            Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    private fun registerNoisyReceiver() {
        if (isNoisyReceiverRegistered) return
        runCatching {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(becomingNoisyReceiver, filter)
            }
            isNoisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!isNoisyReceiverRegistered) return
        runCatching {
            unregisterReceiver(becomingNoisyReceiver)
        }
        isNoisyReceiverRegistered = false
    }

    private fun resolveCoverBitmap(state: PlaybackState): Bitmap? {
        val path = state.coverImagePath
        if (!path.isNullOrBlank()) {
            if (path == cachedCoverPath && cachedCoverBitmap != null) {
                return cachedCoverBitmap
            }
            val file = File(path)
            if (file.exists()) {
                val decoded = runCatching {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    val sampleSize = if (maxDim > 512) (maxDim / 512).coerceAtLeast(1) else 1
                    BitmapFactory.decodeFile(
                        file.absolutePath,
                        BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    )
                }.getOrNull()
                if (decoded != null) {
                    cachedCoverPath = path
                    cachedCoverBitmap = decoded
                    return decoded
                }
            }
        }

        if (state.coverGradient == cachedGradient && cachedCoverBitmap != null && cachedCoverPath == null) {
            return cachedCoverBitmap
        }

        return runCatching {
            val size = 256
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    size.toFloat(),
                    size.toFloat(),
                    intArrayOf(
                        state.coverGradient.startColorHex.toInt(),
                        state.coverGradient.centerColorHex.toInt(),
                        state.coverGradient.endColorHex.toInt()
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            cachedCoverPath = null
            cachedGradient = state.coverGradient
            cachedCoverBitmap = bmp
            bmp
        }.getOrNull()
    }
}
