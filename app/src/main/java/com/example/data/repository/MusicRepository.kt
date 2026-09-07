package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.MusicDatabase
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.PlayerConfigEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class MusicRepository(private val database: MusicDatabase, private val context: Context) {

    val allTracks: Flow<List<TrackEntity>> = database.trackDao().getAllTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = database.playlistDao().getAllPlaylists()
    val history: Flow<List<HistoryEntity>> = database.historyDao().getHistory()
    val playerConfig: Flow<PlayerConfigEntity?> = database.playerConfigDao().getConfig()

    suspend fun getTrackById(id: Long): TrackEntity? = withContext(Dispatchers.IO) {
        database.trackDao().getTrackById(id)
    }

    suspend fun getPlayerConfigSync(): PlayerConfigEntity = withContext(Dispatchers.IO) {
        database.playerConfigDao().getConfigSync() ?: PlayerConfigEntity().also {
            database.playerConfigDao().insertOrUpdateConfig(it)
        }
    }

    suspend fun savePlayerConfig(config: PlayerConfigEntity) = withContext(Dispatchers.IO) {
        database.playerConfigDao().insertOrUpdateConfig(config)
    }

    suspend fun saveHistory(trackId: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        database.historyDao().insertHistory(
            HistoryEntity(
                trackId = trackId,
                lastPositionMs = positionMs,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.historyDao().clearHistory()
    }

    suspend fun createPlaylist(name: String, trackIds: List<Long> = emptyList()) = withContext(Dispatchers.IO) {
        val json = trackIds.joinToString(prefix = "[", postfix = "]")
        database.playlistDao().insertPlaylist(
            PlaylistEntity(playlistName = name, trackIdsJson = json)
        )
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        database.playlistDao().deletePlaylist(playlist)
    }

    /**
     * Queries MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to auto-populate local audio files into Room DB.
     */
    suspend fun scanMediaStore(): Int = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<TrackEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val mediaId = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Track"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    ).toString()

                    val albumArtUri = if (albumIdCol != -1) {
                        val albumId = it.getLong(albumIdCol)
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    tracksList.add(
                        TrackEntity(
                            id = mediaId,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }

            if (tracksList.isNotEmpty()) {
                database.trackDao().insertTracks(tracksList)
                return@withContext tracksList.size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If MediaStore is empty (e.g. Fresh emulator/device), provide demo tracks so user can immediately test
        ensureDemoTracks()
    }

    /**
     * Creates synthesized ambient wav tracks and saves them to Room DB
     * so the app is always instantly playable out of the box.
     */
    private suspend fun ensureDemoTracks(): Int = withContext(Dispatchers.IO) {
        val existing = database.trackDao().getTrackById(1001)
        if (existing != null) return@withContext 0

        val demoDir = File(context.cacheDir, "demo_music").apply { mkdirs() }
        val demoConfigs = listOf(
            Triple("Neumorphic Echoes", "Aura Soundscape", 220.0 to 330.0),
            Triple("Velvet Frequency", "Soft Horizons", 174.0 to 261.0),
            Triple("Cobalt Pulse", "Digital Ambient", 293.0 to 440.0)
        )

        val demoTracks = demoConfigs.mapIndexed { index, (title, artist, freqs) ->
            val wavFile = File(demoDir, "track_${index + 1}.wav")
            if (!wavFile.exists()) {
                generateAmbientWav(wavFile, freqs.first, freqs.second, durationSeconds = 30)
            }
            TrackEntity(
                id = 1001L + index,
                title = title,
                artist = artist,
                album = "Neumorph Studio Vol. 1",
                duration = 30000L,
                contentUri = Uri.fromFile(wavFile).toString(),
                albumArtUri = null
            )
        }

        database.trackDao().insertTracks(demoTracks)
        demoTracks.size
    }

    /**
     * Generates a mellow 16-bit 44.1kHz stereo ambient WAV audio file.
     */
    private fun generateAmbientWav(file: File, freq1: Double, freq2: Double, durationSeconds: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationSeconds
        val numChannels = 2
        val bytesPerSample = 2
        val dataSize = numSamples * numChannels * bytesPerSample

        FileOutputStream(file).use { fos ->
            // RIFF Header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size (16 for PCM)
            header.putShort(1.toShort()) // AudioFormat (1 for PCM)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            header.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            header.putShort(16.toShort()) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)
            fos.write(header.array())

            // Samples buffer
            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            val twoPi = 2.0 * PI

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Gentle tremolo envelope
                val tremolo = (0.7 + 0.3 * sin(twoPi * 0.4 * t))
                val wave1 = sin(twoPi * freq1 * t) * 0.4
                val wave2 = sin(twoPi * freq2 * t) * 0.3
                val harmonic = sin(twoPi * (freq1 * 1.5) * t) * 0.15

                val sampleVal = ((wave1 + wave2 + harmonic) * tremolo * 24000.0).toInt().coerceIn(-32767, 32767).toShort()

                if (buffer.remaining() < 4) {
                    fos.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }

                buffer.putShort(sampleVal) // Left
                buffer.putShort(sampleVal) // Right
            }

            if (buffer.position() > 0) {
                fos.write(buffer.array(), 0, buffer.position())
            }
        }
    }
}
