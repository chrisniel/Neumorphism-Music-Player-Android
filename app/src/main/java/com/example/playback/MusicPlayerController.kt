package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.local.entity.TrackEntity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _eqBass = MutableStateFlow(0)
    val eqBass: StateFlow<Int> = _eqBass.asStateFlow()

    private val _eqMid = MutableStateFlow(0)
    val eqMid: StateFlow<Int> = _eqMid.asStateFlow()

    private val _eqTreble = MutableStateFlow(0)
    val eqTreble: StateFlow<Int> = _eqTreble.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0=Off, 1=All, 2=One
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playlist: StateFlow<List<TrackEntity>> = _playlist.asStateFlow()

    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                mediaController?.let { player ->
                    _isPlaying.value = player.isPlaying
                    _isShuffle.value = player.shuffleModeEnabled
                    _repeatMode.value = player.repeatMode
                    _volume.value = player.volume
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { r -> r.run() })
    }

    private fun setupControllerListener() {
        val player = mediaController ?: return
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                    _currentPositionMs.value = player.currentPosition
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                val found = _playlist.value.find { it.id == mediaId }
                if (found != null) {
                    _currentTrack.value = found
                    _durationMs.value = found.duration
                } else if (mediaItem != null) {
                    _currentTrack.value = TrackEntity(
                        id = mediaId ?: 0L,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "Album",
                        duration = player.duration.coerceAtLeast(0L),
                        contentUri = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    )
                }
                _currentPositionMs.value = player.currentPosition
                if (player.duration > 0) {
                    _durationMs.value = player.duration
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffle.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onVolumeChanged(volume: Float) {
                _volume.value = volume
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaController?.let { player ->
                    _currentPositionMs.value = player.currentPosition
                    if (player.duration > 0 && player.duration != _durationMs.value) {
                        _durationMs.value = player.duration
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: TrackEntity, fullList: List<TrackEntity> = emptyList(), startPositionMs: Long = 0) {
        val player = mediaController ?: return
        if (fullList.isNotEmpty()) {
            _playlist.value = fullList
            val mediaItems = fullList.map { it.toMediaItem() }
            val startIndex = fullList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            player.setMediaItems(mediaItems, startIndex, startPositionMs)
        } else {
            _playlist.value = listOf(track)
            player.setMediaItem(track.toMediaItem(), startPositionMs)
        }
        _currentTrack.value = track
        _durationMs.value = track.duration
        player.prepare()
        player.play()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            val list = _playlist.value
            val current = _currentTrack.value
            if (list.isNotEmpty() && current != null) {
                val nextIdx = (list.indexOfFirst { it.id == current.id } + 1) % list.size
                playTrack(list[nextIdx], list)
            }
        }
    }

    fun skipPrevious() {
        val player = mediaController ?: return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            val list = _playlist.value
            val current = _currentTrack.value
            if (list.isNotEmpty() && current != null) {
                val prevIdx = if (list.indexOfFirst { it.id == current.id } - 1 < 0) list.size - 1 else list.indexOfFirst { it.id == current.id } - 1
                playTrack(list[prevIdx], list)
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaController?.volume = clamped
    }

    fun setEqualizer(bass: Int, mid: Int, treble: Int) {
        _eqBass.value = bass
        _eqMid.value = mid
        _eqTreble.value = treble

        val args = Bundle().apply {
            putInt(MusicPlaybackService.KEY_BASS, bass)
            putInt(MusicPlaybackService.KEY_MID, mid)
            putInt(MusicPlaybackService.KEY_TREBLE, treble)
        }
        val command = SessionCommand(MusicPlaybackService.CUSTOM_CMD_SET_EQ, Bundle.EMPTY)
        mediaController?.sendCustomCommand(command, args)
    }

    fun toggleShuffle() {
        val player = mediaController ?: return
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        _isShuffle.value = newShuffle
    }

    fun toggleRepeat() {
        val player = mediaController ?: return
        val newMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = newMode
        _repeatMode.value = newMode
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    private fun TrackEntity.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }
}
