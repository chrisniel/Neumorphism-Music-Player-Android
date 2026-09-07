package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.data.local.MusicDatabase
import com.example.data.local.entity.PlayerConfigEntity
import com.example.data.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var equalizer: Equalizer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var persistenceJob: Job? = null
    private lateinit var repository: MusicRepository

    companion object {
        const val CUSTOM_CMD_SET_EQ = "com.example.playback.SET_EQ"
        const val KEY_BASS = "key_bass"
        const val KEY_MID = "key_mid"
        const val KEY_TREBLE = "key_treble"
    }

    override fun onCreate() {
        super.onCreate()
        val db = MusicDatabase.getInstance(this)
        repository = MusicRepository(db, this)

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        setupEqualizer()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        startPositionPersistence()
    }

    private fun setupEqualizer() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyEqualizerSettings(bass: Int, mid: Int, treble: Int) {
        val eq = equalizer ?: run {
            setupEqualizer()
            equalizer
        } ?: return

        try {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()

            // Map -100..100 to minLevel..maxLevel
            fun mapLevel(input: Int): Short {
                val clamped = input.coerceIn(-100, 100)
                val ratio = (clamped + 100) / 200f
                return (minLevel + ratio * (maxLevel - minLevel)).toInt().toShort()
            }

            if (numBands > 0) eq.setBandLevel(0.toShort(), mapLevel(bass))
            if (numBands > 1 && numBands < 4) eq.setBandLevel(1.toShort(), mapLevel(mid))
            if (numBands >= 4) {
                eq.setBandLevel(1.toShort(), mapLevel(bass))
                eq.setBandLevel(2.toShort(), mapLevel(mid))
                if (numBands > 3) eq.setBandLevel(3.toShort(), mapLevel(treble))
                if (numBands > 4) eq.setBandLevel(4.toShort(), mapLevel(treble))
            } else if (numBands > 2) {
                eq.setBandLevel((numBands - 1).toShort(), mapLevel(treble))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPositionPersistence() {
        persistenceJob?.cancel()
        persistenceJob = serviceScope.launch {
            while (isActive) {
                delay(3000)
                if (exoPlayer.isPlaying) {
                    val currentMediaItem = exoPlayer.currentMediaItem
                    val currentPos = exoPlayer.currentPosition
                    val trackId = currentMediaItem?.mediaId?.toLongOrNull()

                    if (trackId != null) {
                        repository.saveHistory(trackId, currentPos)
                        val config = repository.getPlayerConfigSync()
                        repository.savePlayerConfig(
                            config.copy(
                                lastPlayedTrackId = trackId,
                                lastPositionMs = currentPos,
                                volume = exoPlayer.volume
                            )
                        )
                    }
                }
            }
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_CMD_SET_EQ) {
                val bass = args.getInt(KEY_BASS, 0)
                val mid = args.getInt(KEY_MID, 0)
                val treble = args.getInt(KEY_TREBLE, 0)
                applyEqualizerSettings(bass, mid, treble)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        persistenceJob?.cancel()
        try {
            equalizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
