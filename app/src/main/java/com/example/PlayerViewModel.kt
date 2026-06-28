package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrackInfo(
    val title: String,
    val artist: String,
    val format: String, // "FLAC", "WAV", "ALAC"
    val sampleRate: String, // "192 kHz", "96 kHz"
    val bitDepth: String, // "24-bit", "32-bit Float"
    val durationSeconds: Int,
    val artworkRes: String
)

class PlayerViewModel : ViewModel() {

    val audioEngine = AudioSynthesizer()

    // Sound tracks
    val playlist = listOf(
        TrackInfo(
            title = "Sub-Bass Shifter (Subwoofer Test)",
            artist = "SubSonic Sound Labs",
            format = "FLAC Lossless",
            sampleRate = "192 kHz",
            bitDepth = "24-bit",
            durationSeconds = 240,
            artworkRes = "img_cyberpunk_album"
        ),
        TrackInfo(
            title = "Hyperdrive DTS Atmos",
            artist = "DTS Spatial Core",
            format = "WAV Lossless",
            sampleRate = "192 kHz",
            bitDepth = "32-bit Float",
            durationSeconds = 180,
            artworkRes = "img_audiophile_bg"
        ),
        TrackInfo(
            title = "Cosmic Dust (Reverb Ambient)",
            artist = "Nebula 9",
            format = "ALAC Lossless",
            sampleRate = "96 kHz",
            bitDepth = "24-bit",
            durationSeconds = 300,
            artworkRes = "img_cyberpunk_album"
        )
    )

    // Observable UI states
    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _trackProgressSeconds = MutableStateFlow(0)
    val trackProgressSeconds: StateFlow<Int> = _trackProgressSeconds.asStateFlow()

    private val _selectedPresetName = MutableStateFlow("Audiophile Flat")
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    // Subwoofer & Bass States
    private val _subwooferVolume = MutableStateFlow(1.2f)
    val subwooferVolume: StateFlow<Float> = _subwooferVolume.asStateFlow()

    private val _subwooferFrequency = MutableStateFlow(55.0f)
    val subwooferFrequency: StateFlow<Float> = _subwooferFrequency.asStateFlow()

    private val _bassBoost = MutableStateFlow(1.8f)
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _phaseInversion = MutableStateFlow(false)
    val phaseInversion: StateFlow<Boolean> = _phaseInversion.asStateFlow()

    // Tone states
    private val _trebleVolume = MutableStateFlow(1.0f)
    val trebleVolume: StateFlow<Float> = _trebleVolume.asStateFlow()

    private val _midVolume = MutableStateFlow(1.0f)
    val midVolume: StateFlow<Float> = _midVolume.asStateFlow()

    private val _masterGain = MutableStateFlow(0.85f)
    val masterGain: StateFlow<Float> = _masterGain.asStateFlow()

    // EQ bands
    private val _eqBands = MutableStateFlow(FloatArray(10) { 1.0f })
    val eqBands: StateFlow<FloatArray> = _eqBands.asStateFlow()

    // Dolby DTS & Spatial Audio
    private val _dolbyDtsEnabled = MutableStateFlow(true)
    val dolbyDtsEnabled: StateFlow<Boolean> = _dolbyDtsEnabled.asStateFlow()

    private val _spatialEnvironment = MutableStateFlow("Concert Hall")
    val spatialEnvironment: StateFlow<String> = _spatialEnvironment.asStateFlow()

    private val _panX = MutableStateFlow(0.0f) // -1.0f (Left) to +1.0f (Right)
    val panX: StateFlow<Float> = _panX.asStateFlow()

    private val _panY = MutableStateFlow(0.5f) // 0.0f (Front) to 1.0f (Rear / Full Depth)
    val panY: StateFlow<Float> = _panY.asStateFlow()

    // Codec Mode
    private val _codecMode = MutableStateFlow("LDAC")
    val codecMode: StateFlow<String> = _codecMode.asStateFlow()

    // Real-time Visualizer feed
    private val _subwooferAmplitude = MutableStateFlow(0.0f)
    val subwooferAmplitude: StateFlow<Float> = _subwooferAmplitude.asStateFlow()

    private val _waveformAmplitude = MutableStateFlow(0.0f)
    val waveformAmplitude: StateFlow<Float> = _waveformAmplitude.asStateFlow()

    private val _waveformBuffer = MutableStateFlow(FloatArray(64) { 0.0f })
    val waveformBuffer: StateFlow<FloatArray> = _waveformBuffer.asStateFlow()

    private var progressJob: Job? = null
    private var visualizerJob: Job? = null

    init {
        // Apply default initialization settings to AudioEngine
        updateAudioEngine()
        applyPreset("Audiophile Flat")
    }

    private fun updateAudioEngine() {
        audioEngine.subwooferFrequency = _subwooferFrequency.value
        audioEngine.subwooferVolume = _subwooferVolume.value
        audioEngine.bassBoost = _bassBoost.value
        audioEngine.trebleVolume = _trebleVolume.value
        audioEngine.midVolume = _midVolume.value
        audioEngine.masterGain = _masterGain.value
        audioEngine.phaseInversion = _phaseInversion.value
        audioEngine.dolbyDtsEnabled = _dolbyDtsEnabled.value
        audioEngine.spatialEnvironment = _spatialEnvironment.value
        audioEngine.panX = _panX.value
        audioEngine.panY = _panY.value
        audioEngine.codecMode = _codecMode.value
        
        val bands = _eqBands.value
        for (i in 0 until 10) {
            audioEngine.eqBands[i] = bands[i]
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        audioEngine.start()
        startTimers()
    }

    fun pause() {
        if (!_isPlaying.value) return
        _isPlaying.value = false
        audioEngine.stop()
        stopTimers()
    }

    fun nextTrack() {
        val nextIdx = (_currentTrackIndex.value + 1) % playlist.size
        selectTrack(nextIdx)
    }

    fun prevTrack() {
        val prevIdx = (_currentTrackIndex.value - 1 + playlist.size) % playlist.size
        selectTrack(prevIdx)
    }

    private fun selectTrack(index: Int) {
        _currentTrackIndex.value = index
        _trackProgressSeconds.value = 0
        
        // If it was playing, restart it
        val wasPlaying = _isPlaying.value
        if (wasPlaying) {
            pause()
            play()
        }
    }

    fun seekProgress(progress: Float) {
        val track = playlist[_currentTrackIndex.value]
        _trackProgressSeconds.value = (progress * track.durationSeconds).toInt().coerceIn(0, track.durationSeconds)
    }

    // Set Subwoofer Properties
    fun setSubwooferVolume(vol: Float) {
        _subwooferVolume.value = vol
        audioEngine.subwooferVolume = vol
    }

    fun setSubwooferFrequency(freq: Float) {
        _subwooferFrequency.value = freq
        audioEngine.subwooferFrequency = freq
    }

    fun setBassBoost(boost: Float) {
        _bassBoost.value = boost
        audioEngine.bassBoost = boost
    }

    fun togglePhaseInversion() {
        val next = !_phaseInversion.value
        _phaseInversion.value = next
        audioEngine.phaseInversion = next
    }

    // Set Tones
    fun setTrebleVolume(vol: Float) {
        _trebleVolume.value = vol
        audioEngine.trebleVolume = vol
    }

    fun setMidVolume(vol: Float) {
        _midVolume.value = vol
        audioEngine.midVolume = vol
    }

    fun setMasterGain(gain: Float) {
        _masterGain.value = gain
        audioEngine.masterGain = gain
    }

    // Spatial and Dolby
    fun toggleDolbyDts() {
        val next = !_dolbyDtsEnabled.value
        _dolbyDtsEnabled.value = next
        audioEngine.dolbyDtsEnabled = next
    }

    fun setSpatialEnvironment(env: String) {
        _spatialEnvironment.value = env
        audioEngine.spatialEnvironment = env
    }

    fun set3DPosition(x: Float, y: Float) {
        _panX.value = x
        _panY.value = y
        audioEngine.panX = x
        audioEngine.panY = y
    }

    // Codec switcher
    fun setCodecMode(mode: String) {
        _codecMode.value = mode
        audioEngine.codecMode = mode
    }

    // EQ controls
    fun updateEqBand(index: Int, gain: Float) {
        val current = _eqBands.value.copyOf()
        current[index] = gain
        _eqBands.value = current
        audioEngine.eqBands[index] = gain
        _selectedPresetName.value = "Custom EQ"
    }

    fun applyPreset(presetName: String) {
        _selectedPresetName.value = presetName
        val newBands = when (presetName) {
            "Audiophile Flat" -> FloatArray(10) { 1.0f }
            "Subwoofer Heavy" -> floatArrayOf(2.5f, 2.2f, 1.8f, 1.3f, 1.0f, 0.9f, 0.8f, 0.7f, 0.7f, 0.6f)
            "Acoustic Live" -> floatArrayOf(0.8f, 0.9f, 1.1f, 1.3f, 1.5f, 1.7f, 1.8f, 1.9f, 1.5f, 1.2f)
            "Dolby Theatre" -> floatArrayOf(2.2f, 1.6f, 1.0f, 0.8f, 1.1f, 1.4f, 1.8f, 2.0f, 2.2f, 2.4f)
            "Vocal Clarity" -> floatArrayOf(0.6f, 0.7f, 0.9f, 1.5f, 2.0f, 2.0f, 1.6f, 1.2f, 1.0f, 0.8f)
            "Electro Synth" -> floatArrayOf(2.3f, 2.0f, 1.2f, 0.8f, 1.1f, 1.4f, 1.7f, 2.0f, 2.2f, 2.4f)
            else -> FloatArray(10) { 1.0f }
        }
        _eqBands.value = newBands
        for (i in 0 until 10) {
            audioEngine.eqBands[i] = newBands[i]
        }
    }

    private fun startTimers() {
        stopTimers()
        
        // Progress updater loop
        progressJob = viewModelScope.launch(Dispatchers.Default) {
            val track = playlist[_currentTrackIndex.value]
            while (_isPlaying.value) {
                delay(1000)
                val currentSec = _trackProgressSeconds.value
                if (currentSec >= track.durationSeconds) {
                    // Loop or next track
                    _trackProgressSeconds.value = 0
                } else {
                    _trackProgressSeconds.value = currentSec + 1
                }
            }
        }

        // Fast visualization poller loop (60 FPS)
        visualizerJob = viewModelScope.launch(Dispatchers.Main) {
            while (_isPlaying.value) {
                _subwooferAmplitude.value = audioEngine.lastSubwooferAmplitude
                _waveformAmplitude.value = audioEngine.lastWaveformAmplitude
                _waveformBuffer.value = audioEngine.visualizerBuffer.copyOf()
                delay(16) // ~60 FPS
            }
        }
    }

    private fun stopTimers() {
        progressJob?.cancel()
        progressJob = null
        visualizerJob?.cancel()
        visualizerJob = null
        
        // Reset amplitudes to rest state on pause
        _subwooferAmplitude.value = 0.0f
        _waveformAmplitude.value = 0.0f
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
