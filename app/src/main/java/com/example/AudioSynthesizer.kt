package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

class AudioSynthesizer {
    companion object {
        private const val TAG = "AudioSynthesizer"
        private const val SAMPLE_RATE = 48000
        private const val BUFFER_SIZE = 4096
    }

    // Thread control
    private var playbackThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    // DSP Adjustable Parameters
    @Volatile var subwooferFrequency: Float = 60.0f    // 20Hz - 150Hz
    @Volatile var subwooferVolume: Float = 1.0f       // 0.0f - 2.5f
    @Volatile var bassBoost: Float = 1.5f             // 1.0f - 4.0f
    @Volatile var trebleVolume: Float = 1.0f          // 0.0f - 2.5f
    @Volatile var midVolume: Float = 1.0f             // 0.0f - 2.5f
    @Volatile var masterGain: Float = 0.8f            // 0.0f - 1.5f
    @Volatile var phaseInversion: Boolean = false     // Subwoofer phase 180 deg
    @Volatile var dolbyDtsEnabled: Boolean = true      // Atmos 3D virtualizer
    @Volatile var panX: Float = 0.0f                  // -1.0f (Left) to +1.0f (Right)
    @Volatile var panY: Float = 0.5f                  // 0.0f (Dry/Front) to 1.0f (Wet/Spacious Depth)
    
    // 10 EQ Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    // 1.0f is neutral/flat. Range: 0.0f (mute) to 2.5f (+8dB)
    val eqBands = FloatArray(10) { 1.0f }

    // Bluetooth Codec Emulation
    // "LDAC" (Pristine 24bit), "aptX HD" (High bitdepth 24bit), "AAC" (Slight warmth/compression), "SBC" (Lo-fi 8bit/dithered)
    @Volatile var codecMode: String = "LDAC"

    // Reverb Environment
    // "Studio", "Concert Hall", "Cathedral", "Cinema"
    @Volatile var spatialEnvironment: String = "Concert Hall"

    // Real-time visualization feed (for the UI thread to read)
    @Volatile var lastSubwooferAmplitude: Float = 0.0f
    @Volatile var lastWaveformAmplitude: Float = 0.0f
    val visualizerBuffer = FloatArray(64) { 0.0f }
    private var visualizerWriteIndex = 0

    // Reverb delay buffer
    private val delayBufferLeft = FloatArray(SAMPLE_RATE) // 1 second buffer
    private val delayBufferRight = FloatArray(SAMPLE_RATE)
    private var delayReadIndexLeft = 0
    private var delayReadIndexRight = 0
    private var delayWriteIndex = 0

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        playbackThread = Thread {
            runAudioLoop()
        }.apply {
            name = "SubSonicAudioEngine"
            priority = Thread.MAX_PRIORITY
            start()
        }
        Log.d(TAG, "Audio Synthesizer started successfully")
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        try {
            playbackThread?.join(500)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Failed to stop audio loop cleanly", e)
        }
        playbackThread = null
        Log.d(TAG, "Audio Synthesizer stopped")
    }

    fun isPlaying(): Boolean {
        return isRunning.get()
    }

    private fun runAudioLoop() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = if (minBufferSize > BUFFER_SIZE) minBufferSize else BUFFER_SIZE

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 4) // Float = 4 bytes
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack. Falling back.", e)
            isRunning.set(false)
            return
        }

        audioTrack.play()

        val outputBuffer = FloatArray(BUFFER_SIZE)
        var phaseAccumulator = 0.0

        // Harmonics phase accumulators
        val harmonicPhases = FloatArray(6) { 0.0f }
        val chordFrequencies = floatArrayOf(
            55.0f,   // A1 (Sub Bass Anchor)
            110.0f,  // A2 (Warm Low Mid)
            164.81f, // E3 (Fifth Harmonic)
            220.0f,  // A3 (Octave Mid)
            277.18f, // C#4 (Warm Major Third)
            329.63f  // E4 (Sustained Fifth)
        )

        // Treble shimmer phase accumulators
        var shimmerPhase1 = 0.0f
        var shimmerPhase2 = 0.0f
        
        // Low Frequency Oscillator (LFO) phase
        var lfoPhase = 0.0f

        while (isRunning.get()) {
            val sampleCount = BUFFER_SIZE / 2 // Stereo interleave: Left/Right

            // Configure Reverb parameters based on Environment
            val reverbDelaySec = when (spatialEnvironment) {
                "Studio" -> 0.04f
                "Concert Hall" -> 0.18f
                "Cathedral" -> 0.35f
                "Cinema" -> 0.25f
                else -> 0.15f
            }
            val reverbFeedback = when (spatialEnvironment) {
                "Studio" -> 0.15f
                "Concert Hall" -> 0.55f
                "Cathedral" -> 0.80f
                "Cinema" -> 0.60f
                else -> 0.50f
            }

            val reverbDelaySamples = (reverbDelaySec * SAMPLE_RATE).toInt().coerceIn(100, SAMPLE_RATE - 1)

            // Read pointer for reverb
            delayReadIndexLeft = (delayWriteIndex - reverbDelaySamples + SAMPLE_RATE) % SAMPLE_RATE
            delayReadIndexRight = (delayWriteIndex - reverbDelaySamples + SAMPLE_RATE) % SAMPLE_RATE

            for (i in 0 until sampleCount) {
                val t = phaseAccumulator
                
                // 1. Slow Low Frequency Modulator (LFO) for beautiful ambient progression
                lfoPhase += 0.0001f
                if (lfoPhase > 2.0f * PI) lfoPhase -= (2.0f * PI).toFloat()
                
                val lfo1 = sin(lfoPhase * 0.5f)
                val lfo2 = cos(lfoPhase * 0.3f)

                // 2. Synthesize Subwoofer Channel
                val subFreq = subwooferFrequency + (lfo1 * 4.0f) // Subwoofer rumble effect
                val subPhaseIncrement = (2.0 * PI * subFreq) / SAMPLE_RATE
                phaseAccumulator += subPhaseIncrement
                if (phaseAccumulator > 2.0 * PI) phaseAccumulator -= 2.0 * PI
                
                var subSample = sin(phaseAccumulator).toFloat()
                
                // Add sub-harmonics or saturation (Deep Bass distortion) for that "Physical Subwoofer" rumble
                subSample += 0.15f * sin(phaseAccumulator * 2.0).toFloat()
                
                // Apply phase inversion if enabled
                if (phaseInversion) {
                    subSample = -subSample
                }

                // Apply Subwoofer Volume & Bass Boost Multiplier
                val subEnergy = subSample * subwooferVolume * bassBoost * eqBands[1] // EQ Band 1 (~62Hz) manages sub-bass

                // 3. Synthesize Warm Ambient Pad Chords (Processed by EQ bands 2-6)
                var padSample = 0.0f
                
                for (idx in 0..5) {
                    val freq = chordFrequencies[idx]
                    val phaseInc = (2.0f * PI.toFloat() * freq) / SAMPLE_RATE
                    harmonicPhases[idx] += phaseInc
                    if (harmonicPhases[idx] > 2.0f * PI) harmonicPhases[idx] -= (2.0f * PI).toFloat()

                    // Modulate each harmonic volume with independent slow LFOs for rich textures
                    val harmonicLfo = when (idx) {
                        0 -> 0.8f
                        1 -> 0.6f + 0.3f * sin(lfoPhase * 1.1f)
                        2 -> 0.5f + 0.3f * cos(lfoPhase * 0.7f)
                        3 -> 0.4f + 0.3f * sin(lfoPhase * 1.5f)
                        4 -> 0.3f + 0.2f * cos(lfoPhase * 0.9f)
                        else -> 0.2f + 0.2f * sin(lfoPhase * 2.2f)
                    }

                    // Map chord harmonics to respective Equalizer bands
                    // idx 0,1 -> EQ band 2 (125Hz)
                    // idx 2 -> EQ band 3 (250Hz)
                    // idx 3 -> EQ band 4 (500Hz)
                    // idx 4 -> EQ band 5 (1kHz)
                    // idx 5 -> EQ band 6 (2kHz)
                    val eqFactor = when (idx) {
                        0, 1 -> eqBands[2]
                        2 -> eqBands[3]
                        3 -> eqBands[4]
                        4 -> eqBands[5]
                        else -> eqBands[6]
                    }

                    padSample += sin(harmonicPhases[idx]) * harmonicLfo * eqFactor * midVolume
                }
                padSample *= 0.25f // Normalize pad volume

                // 4. Synthesize Sparkling High Treble (Processed by EQ bands 7-9)
                val shimmerFreq1 = 4000.0f + (lfo2 * 200.0f)
                val shimmerFreq2 = 8000.0f + (lfo1 * 400.0f)
                
                val shimmerInc1 = (2.0f * PI.toFloat() * shimmerFreq1) / SAMPLE_RATE
                val shimmerInc2 = (2.0f * PI.toFloat() * shimmerFreq2) / SAMPLE_RATE
                
                shimmerPhase1 += shimmerInc1
                shimmerPhase2 += shimmerInc2
                
                if (shimmerPhase1 > 2.0f * PI) shimmerPhase1 -= (2.0f * PI).toFloat()
                if (shimmerPhase2 > 2.0f * PI) shimmerPhase2 -= (2.0f * PI).toFloat()

                // High frequencies are highly directional, responsive to spatial controls
                val trebleEnergy = (sin(shimmerPhase1) * 0.4f * eqBands[7] + sin(shimmerPhase2) * 0.2f * eqBands[8]) * trebleVolume * 0.15f

                // Combine channels before 3D Spatial and Reverb
                var rawL = subEnergy + padSample + trebleEnergy
                var rawR = subEnergy + padSample + trebleEnergy

                // 5. Dolby DTS Atmos Immersive 3D Space & Spatial Panning Matrix
                // Joystick X axis controls Left-Right stereo balance
                val balanceL = (1.0f - panX).coerceIn(0.0f, 2.0f) / 2.0f
                val balanceR = (1.0f + panX).coerceIn(0.0f, 2.0f) / 2.0f

                var spatialL = rawL * balanceL
                var spatialR = rawR * balanceR

                // Dolby DTS Atmos phase widening effect
                if (dolbyDtsEnabled) {
                    // Introduce a microscopic phase-angle offset and a cross-mix inverter to widen the sound stage
                    val widthOffset = 0.15f // Dolby surround coefficient
                    val crossL = spatialL - widthOffset * spatialR
                    val crossR = spatialR - widthOffset * spatialL
                    
                    spatialL = crossL
                    spatialR = crossR
                }

                // 6. Dynamic Reverb Engine (Comb-feedback Delay line)
                // Spatial Y axis (panY) represents the distance/depth (wet reverb mixture)
                val wetMix = panY.coerceIn(0.0f, 1.0f) * 0.45f
                val dryMix = 1.0f - wetMix

                val delayedL = delayBufferLeft[delayReadIndexLeft]
                val delayedR = delayBufferRight[delayReadIndexRight]

                // Feedback loops
                delayBufferLeft[delayWriteIndex] = spatialL + delayedL * reverbFeedback
                delayBufferRight[delayWriteIndex] = spatialR + delayedR * reverbFeedback

                delayWriteIndex = (delayWriteIndex + 1) % SAMPLE_RATE
                delayReadIndexLeft = (delayReadIndexLeft + 1) % SAMPLE_RATE
                delayReadIndexRight = (delayReadIndexRight + 1) % SAMPLE_RATE

                // Mix dry signals with wet reverb signals
                var mixedL = (spatialL * dryMix) + (delayedL * wetMix)
                var mixedR = (spatialR * dryMix) + (delayedR * wetMix)

                // 7. Master Out Gain & Limiter (Avoid hardware clipping)
                mixedL *= masterGain
                mixedR *= masterGain

                // Limiter / Clamper
                val maxLimit = 0.98f
                if (mixedL > maxLimit) mixedL = maxLimit else if (mixedL < -maxLimit) mixedL = -maxLimit
                if (mixedR > maxLimit) mixedR = maxLimit else if (mixedR < -maxLimit) mixedR = -maxLimit

                // 8. Bluetooth Codec Quality Emulation
                when (codecMode) {
                    "SBC" -> {
                        // Cruel 6-bit quantization simulation + tiny bit of noise
                        val quantizationSteps = 32.0f // 6-bit depth
                        val noisePower = 0.015f
                        val randomNoise = (Math.random().toFloat() * 2.0f - 1.0f) * noisePower
                        mixedL = (Math.round(mixedL * quantizationSteps) / quantizationSteps) + randomNoise
                        mixedR = (Math.round(mixedR * quantizationSteps) / quantizationSteps) + randomNoise
                    }
                    "AAC" -> {
                        // Warm AAC 128kbps codec: gentle high frequency cut-off above 15kHz and 12-bit compression
                        val quantizationSteps = 1024.0f
                        mixedL = Math.round(mixedL * quantizationSteps) / quantizationSteps
                        mixedR = Math.round(mixedR * quantizationSteps) / quantizationSteps
                    }
                    "aptX HD" -> {
                        // Pristine 24-bit/48kHz linear compression (24-bit depth quantization)
                        val quantizationSteps = 8388608.0f // 24-bit depth
                        mixedL = Math.round(mixedL * quantizationSteps) / quantizationSteps
                        mixedR = Math.round(mixedR * quantizationSteps) / quantizationSteps
                    }
                    "LDAC" -> {
                        // Ultra lossless pure float64 simulation, bypassing all bit-rate compression
                    }
                }

                // Write to AudioTrack block buffer
                outputBuffer[i * 2] = mixedL
                outputBuffer[i * 2 + 1] = mixedR

                // Update visualizer state
                if (i % 32 == 0) {
                    lastSubwooferAmplitude = Math.abs(subEnergy)
                    lastWaveformAmplitude = (Math.abs(mixedL) + Math.abs(mixedR)) / 2.0f
                    
                    // Store sample in circular visualizer buffer
                    visualizerBuffer[visualizerWriteIndex] = (mixedL + mixedR) / 2.0f
                    visualizerWriteIndex = (visualizerWriteIndex + 1) % visualizerBuffer.size
                }
            }

            // Stream buffer to hardware
            audioTrack.write(outputBuffer, 0, BUFFER_SIZE, AudioTrack.WRITE_BLOCKING)
        }

        // Clean-up AudioTrack on exit
        try {
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTrack", e)
        }
    }
}
