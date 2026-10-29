package com.vietreader.tts

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.vietreader.tts.data.repository.BookRepositoryImpl
import com.vietreader.tts.service.PlaybackForegroundService
import com.vietreader.tts.ui.navigation.WordwaveNavGraph
import com.vietreader.tts.ui.theme.VietReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val openReaderRequestTick = MutableStateFlow(0L)
    private var playbackService: PlaybackForegroundService? = null
    private var isServiceBound: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? PlaybackForegroundService.LocalBinder
            playbackService = binder?.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && BookRepositoryImpl.instance.playbackState.value?.isPlaying == true) {
            PlaybackForegroundService.startOrUpdate(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BookRepositoryImpl.instance.initialize(applicationContext)
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appSettings by BookRepositoryImpl.instance.appSettings.collectAsState()
            val openReaderTick by openReaderRequestTick.collectAsState()
            VietReaderTheme(themeMode = appSettings.themeMode) {
                WordwaveNavGraph(
                    openReaderRequestTick = openReaderTick,
                    onOpenReaderHandled = { openReaderRequestTick.value = 0L }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val bindIntent = Intent(this, PlaybackForegroundService::class.java)
        runCatching {
            bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        if (isServiceBound) {
            runCatching { unbindService(serviceConnection) }
            isServiceBound = false
            playbackService = null
        }
        super.onStop()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PlaybackForegroundService.EXTRA_OPEN_READER, false) == true) {
            val targetBookId = intent.getStringExtra(PlaybackForegroundService.EXTRA_BOOK_ID)
            if (!targetBookId.isNullOrBlank()) {
                BookRepositoryImpl.instance.selectBookForPlayback(targetBookId)
            }
            openReaderRequestTick.value = System.currentTimeMillis()
            intent.removeExtra(PlaybackForegroundService.EXTRA_OPEN_READER)
        }
    }
}
