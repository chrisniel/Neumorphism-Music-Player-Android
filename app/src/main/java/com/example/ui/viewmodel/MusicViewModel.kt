package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MusicDatabase
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.PlayerConfigEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.TrackEntity
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqPreset(
    val name: String,
    val bass: Int,
    val mid: Int,
    val treble: Int
)

val PRESETS = listOf(
    EqPreset("Flat", 0, 0, 0),
    EqPreset("Bass Boost", 80, 10, -20),
    EqPreset("Rock", 60, -10, 50),
    EqPreset("Pop", 20, 50, 40),
    EqPreset("Vocal", -20, 80, 20),
    EqPreset("Electronic", 70, 0, 60)
)

data class MusicUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.8f,
    val eqBass: Int = 0,
    val eqMid: Int = 0,
    val eqTreble: Int = 0,
    val selectedPresetName: String = "Flat",
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0,
    val activeTab: Int = 0, // 0 = Player, 1 = EQ, 2 = Library
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val statusMessage: String? = null
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getInstance(application)
    val repository = MusicRepository(database, application)
    val controller = MusicPlayerController(application, viewModelScope)

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        // Collect DB updates
        viewModelScope.launch {
            repository.allTracks.collect { tracksList ->
                _uiState.value = _uiState.value.copy(tracks = tracksList)
                // If no current track is loaded yet, restore from last config or pick first track
                if (_uiState.value.currentTrack == null && tracksList.isNotEmpty()) {
                    val config = repository.getPlayerConfigSync()
                    val savedTrack = tracksList.find { it.id == config.lastPlayedTrackId } ?: tracksList.first()
                    _uiState.value = _uiState.value.copy(
                        currentTrack = savedTrack,
                        durationMs = savedTrack.duration,
                        currentPositionMs = config.lastPositionMs,
                        volume = config.volume,
                        eqBass = config.eqBass,
                        eqMid = config.eqMid,
                        eqTreble = config.eqTreble
                    )
                    controller.setVolume(config.volume)
                    controller.setEqualizer(config.eqBass, config.eqMid, config.eqTreble)
                }
            }
        }

        viewModelScope.launch {
            repository.allPlaylists.collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }

        viewModelScope.launch {
            repository.history.collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }

        // Collect Player Controller state
        viewModelScope.launch {
            controller.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }

        viewModelScope.launch {
            controller.currentTrack.collect { track ->
                if (track != null) {
                    _uiState.value = _uiState.value.copy(
                        currentTrack = track,
                        durationMs = if (track.duration > 0) track.duration else _uiState.value.durationMs
                    )
                }
            }
        }

        viewModelScope.launch {
            controller.currentPositionMs.collect { pos ->
                _uiState.value = _uiState.value.copy(currentPositionMs = pos)
            }
        }

        viewModelScope.launch {
            controller.durationMs.collect { dur ->
                if (dur > 0) {
                    _uiState.value = _uiState.value.copy(durationMs = dur)
                }
            }
        }

        viewModelScope.launch {
            controller.volume.collect { vol ->
                _uiState.value = _uiState.value.copy(volume = vol)
            }
        }

        viewModelScope.launch {
            controller.isShuffle.collect { shuffle ->
                _uiState.value = _uiState.value.copy(isShuffle = shuffle)
            }
        }

        viewModelScope.launch {
            controller.repeatMode.collect { repeat ->
                _uiState.value = _uiState.value.copy(repeatMode = repeat)
            }
        }

        // Auto scan MediaStore on launch
        scanDeviceMusic()
    }

    fun scanDeviceMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = "Scanning audio files...")
            val count = repository.scanMediaStore()
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                statusMessage = if (count > 0) "Loaded $count tracks" else "Audio library ready"
            )
        }
    }

    fun playTrack(track: TrackEntity) {
        val all = _uiState.value.tracks
        controller.playTrack(track, all)
        _uiState.value = _uiState.value.copy(currentTrack = track)
    }

    fun togglePlayPause() {
        val current = _uiState.value.currentTrack
        if (current != null) {
            if (controller.currentTrack.value == null) {
                controller.playTrack(current, _uiState.value.tracks, _uiState.value.currentPositionMs)
            } else {
                controller.togglePlayPause()
            }
        } else if (_uiState.value.tracks.isNotEmpty()) {
            playTrack(_uiState.value.tracks.first())
        }
    }

    fun skipNext() {
        controller.skipNext()
    }

    fun skipPrevious() {
        controller.skipPrevious()
    }

    fun seekToRatio(ratio: Float) {
        val dur = _uiState.value.durationMs
        if (dur > 0) {
            val targetMs = (dur * ratio).toLong().coerceIn(0L, dur)
            controller.seekTo(targetMs)
        }
    }

    fun setVolume(volume: Float) {
        controller.setVolume(volume)
        saveCurrentConfig()
    }

    fun setBass(ratio: Float) {
        // ratio 0..1 -> -100..100
        val bass = ((ratio * 200) - 100).toInt().coerceIn(-100, 100)
        _uiState.value = _uiState.value.copy(eqBass = bass, selectedPresetName = "Custom")
        controller.setEqualizer(bass, _uiState.value.eqMid, _uiState.value.eqTreble)
        saveCurrentConfig()
    }

    fun setMid(ratio: Float) {
        val mid = ((ratio * 200) - 100).toInt().coerceIn(-100, 100)
        _uiState.value = _uiState.value.copy(eqMid = mid, selectedPresetName = "Custom")
        controller.setEqualizer(_uiState.value.eqBass, mid, _uiState.value.eqTreble)
        saveCurrentConfig()
    }

    fun setTreble(ratio: Float) {
        val treble = ((ratio * 200) - 100).toInt().coerceIn(-100, 100)
        _uiState.value = _uiState.value.copy(eqTreble = treble, selectedPresetName = "Custom")
        controller.setEqualizer(_uiState.value.eqBass, _uiState.value.eqMid, treble)
        saveCurrentConfig()
    }

    fun applyPreset(preset: EqPreset) {
        _uiState.value = _uiState.value.copy(
            eqBass = preset.bass,
            eqMid = preset.mid,
            eqTreble = preset.treble,
            selectedPresetName = preset.name
        )
        controller.setEqualizer(preset.bass, preset.mid, preset.treble)
        saveCurrentConfig()
    }

    fun toggleShuffle() {
        controller.toggleShuffle()
        saveCurrentConfig()
    }

    fun toggleRepeat() {
        controller.toggleRepeat()
        saveCurrentConfig()
    }

    fun setActiveTab(index: Int) {
        _uiState.value = _uiState.value.copy(activeTab = index)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name.trim())
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun saveCurrentConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.savePlayerConfig(
                PlayerConfigEntity(
                    id = 1,
                    volume = state.volume,
                    eqBass = state.eqBass,
                    eqMid = state.eqMid,
                    eqTreble = state.eqTreble,
                    isShuffle = state.isShuffle,
                    repeatMode = state.repeatMode,
                    lastPlayedTrackId = state.currentTrack?.id,
                    lastPositionMs = state.currentPositionMs
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller.release()
    }
}
