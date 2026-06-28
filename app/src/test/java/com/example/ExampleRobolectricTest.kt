package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SubSonic", appName)
    }

    @Test
    fun `verify PlayerViewModel initial states`() {
        val viewModel = PlayerViewModel()
        assertFalse(viewModel.isPlaying.value)
        assertEquals(0, viewModel.currentTrackIndex.value)
        assertEquals("Audiophile Flat", viewModel.selectedPresetName.value)
    }

    @Test
    fun `verify PlayerViewModel playback states`() {
        val viewModel = PlayerViewModel()
        
        // Starts paused
        assertFalse(viewModel.isPlaying.value)
        
        // Toggle play
        viewModel.togglePlayback()
        assertTrue(viewModel.isPlaying.value)
        
        // Toggle pause
        viewModel.togglePlayback()
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun `verify Equalizer presets computation`() {
        val viewModel = PlayerViewModel()
        
        // Check initial bands are flat (1.0)
        val initialBands = viewModel.eqBands.value
        initialBands.forEach { assertEquals(1.0f, it) }
        
        // Apply Subwoofer Heavy preset
        viewModel.applyPreset("Subwoofer Heavy")
        assertEquals("Subwoofer Heavy", viewModel.selectedPresetName.value)
        
        // Band 0 (31Hz) and Band 1 (62Hz) should be heavily boosted
        val boostedBands = viewModel.eqBands.value
        assertTrue(boostedBands[0] > 2.0f)
        assertTrue(boostedBands[1] > 2.0f)
    }

    @Test
    fun `verify Bluetooth Codec updates`() {
        val viewModel = PlayerViewModel()
        
        // Default codec is LDAC
        assertEquals("LDAC", viewModel.codecMode.value)
        
        // Set to compressed SBC
        viewModel.setCodecMode("SBC")
        assertEquals("SBC", viewModel.codecMode.value)
        assertEquals("SBC", viewModel.audioEngine.codecMode)
    }

    @Test
    fun `verify 3D Spatial Audio and Tone settings`() {
        val viewModel = PlayerViewModel()
        
        // Default spatial state
        assertTrue(viewModel.dolbyDtsEnabled.value)
        
        // Toggle Dolby DTS
        viewModel.toggleDolbyDts()
        assertFalse(viewModel.dolbyDtsEnabled.value)
        assertFalse(viewModel.audioEngine.dolbyDtsEnabled)
        
        // Set spatial coordinate panning
        viewModel.set3DPosition(0.75f, 0.25f)
        assertEquals(0.75f, viewModel.panX.value)
        assertEquals(0.25f, viewModel.panY.value)
    }
}
