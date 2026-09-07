package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: String,
    val albumArtUri: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistName: String,
    val trackIdsJson: String = "[]"
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val lastPositionMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_config")
data class PlayerConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val volume: Float = 0.8f,
    val eqBass: Int = 0,
    val eqMid: Int = 0,
    val eqTreble: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0, // 0 = OFF, 1 = ALL, 2 = ONE
    val lastPlayedTrackId: Long? = null,
    val lastPositionMs: Long = 0L
)
