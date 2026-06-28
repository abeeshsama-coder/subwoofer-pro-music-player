package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SpaceBlack
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTrackIdx by viewModel.currentTrackIndex.collectAsState()
    val track = viewModel.playlist[currentTrackIdx]

    // Navigation state
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Now Playing", "Bass & DSP", "Equalizer", "Atmos & Codec")

    // Master configuration for adaptive screen sizing
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        // Horizontal Adaptive Layout for widescreen/tablet
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(SpaceBlack)
        ) {
            // Navigation Rail on tablet
            NavigationRail(
                containerColor = SlateDark,
                contentColor = TextPrimary,
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "App Icon",
                    tint = BassPurple,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(40.dp))
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.PlayArrow
                                    1 -> Icons.Default.Speaker
                                    2 -> Icons.Default.Tune
                                    else -> Icons.Default.Bluetooth
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title, fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = SubCyan,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = SubCyan,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = SlateMedium
                        ),
                        modifier = Modifier.testTag("nav_rail_item_$index")
                    )
                }
            }

            // Main Content Area with Split Pane
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Persistent mini-player or details
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .background(SlateDark)
                        .border(1.dp, SlateLight, RoundedCornerShape(0.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MONITOR ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HiResGold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            // Compact Art
                            CompactSubwooferVisualizer(viewModel = viewModel)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = track.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = track.artist,
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Compact playback status
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FormatBadge(track = track)
                            Spacer(modifier = Modifier.height(16.dp))
                            PlaybackControlsRow(viewModel = viewModel, isCompact = true)
                        }
                    }
                }

                // Dynamic Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    when (selectedTab) {
                        0 -> NowPlayingTab(viewModel = viewModel)
                        1 -> BassDspTab(viewModel = viewModel)
                        2 -> EqualizerTab(viewModel = viewModel)
                        3 -> AtmosCodecTab(viewModel = viewModel)
                    }
                }
            }
        }
    } else {
        // Standard Mobile Layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SpaceBlack)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Logo",
                        tint = BassPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUBSONIC",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(BassPurple, SubCyan)),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DOLBY ATMOS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpaceBlack
                        )
                    }
                }
            }

            // Tabs / Swipeable navigation header
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SpaceBlack,
                contentColor = TextPrimary,
                edgePadding = 16.dp,
                divider = { Divider(color = SlateMedium) },
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SubCyan,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) TextPrimary else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("tab_button_$index")
                    )
                }
            }

            // Tab Content Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (selectedTab) {
                    0 -> NowPlayingTab(viewModel = viewModel)
                    1 -> BassDspTab(viewModel = viewModel)
                    2 -> EqualizerTab(viewModel = viewModel)
                    3 -> AtmosCodecTab(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 1. NOW PLAYING TAB
// ==========================================
@Composable
fun NowPlayingTab(viewModel: PlayerViewModel) {
    val currentTrackIdx by viewModel.currentTrackIndex.collectAsState()
    val track = viewModel.playlist[currentTrackIdx]
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressSeconds by viewModel.trackProgressSeconds.collectAsState()

    val progressFraction = progressSeconds.toFloat() / track.durationSeconds.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Visualizer & Subwoofer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, SlateLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Futuristic background wave artwork
                    Image(
                        painter = painterResource(id = R.drawable.img_audiophile_bg),
                        contentDescription = "Background Acoustic Field",
                        contentScale = ContentScale.Crop,
                        alpha = 0.25f,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Interactive Subwoofer Cone Visualizer
                    FullSubwooferVisualizer(viewModel = viewModel)
                }
            }
        }

        // Song Metadata & Lossless Badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                }
                
                // Lossless Audio Signal Badge
                FormatBadge(track = track)
            }
        }

        // Timeline Slider
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressFraction,
                    onValueChange = { viewModel.seekProgress(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = SubCyan,
                        activeTrackColor = SubCyan,
                        inactiveTrackColor = SlateMedium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_progress_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progressSeconds),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text(
                        text = formatTime(track.durationSeconds),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }

        // Playback Controllers
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PlaybackControlsRow(viewModel = viewModel, isCompact = false)
            }
        }

        // Quick Subwoofer Booster Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                val subwooferVol by viewModel.subwooferVolume.collectAsState()
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = "Subwoofer",
                                tint = BassPurple
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quick Subwoofer Feed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${(subwooferVol * 100).toInt()}% Gain",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BassPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = subwooferVol,
                        onValueChange = { viewModel.setSubwooferVolume(it) },
                        valueRange = 0.0f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = BassPurple,
                            activeTrackColor = BassPurple,
                            inactiveTrackColor = SlateDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subwoofer_quick_volume")
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// 2. BASS & DSP MASTER TAB
// ==========================================
@Composable
fun BassDspTab(viewModel: PlayerViewModel) {
    val subwooferVol by viewModel.subwooferVolume.collectAsState()
    val subwooferFreq by viewModel.subwooferFrequency.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val phaseInversion by viewModel.phaseInversion.collectAsState()
    val trebleVol by viewModel.trebleVolume.collectAsState()
    val midVol by viewModel.midVolume.collectAsState()
    val masterGain by viewModel.masterGain.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Subwoofer Crossover card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, BassPurple.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DSP SUBWOOFER CROSSOVER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BassPurple,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Virtual Subwoofer Cabinet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Switch(
                            checked = subwooferVol > 0.1f,
                            onCheckedChange = { if (it) viewModel.setSubwooferVolume(1.2f) else viewModel.setSubwooferVolume(0.0f) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BassPurple,
                                checkedTrackColor = BassPurple.copy(alpha = 0.3f),
                                uncheckedThumbColor = SlateLight,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("subwoofer_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated real-time filter response display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(SpaceBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateLight, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val crossoverPx = (subwooferFreq / 150f) * w
                            
                            val p = Path().apply {
                                moveTo(0f, h * 0.2f)
                                lineTo(crossoverPx * 0.8f, h * 0.2f)
                                cubicTo(
                                    crossoverPx, h * 0.2f,
                                    crossoverPx + 20f, h * 0.9f,
                                    w, h * 0.9f
                                )
                            }
                            drawPath(
                                path = p,
                                color = BassPurple,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // Draw Crossover Line
                            drawLine(
                                color = SubCyan,
                                start = Offset(crossoverPx, 0f),
                                end = Offset(crossoverPx, h),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                        
                        Text(
                            text = "LFE Cutoff: ${subwooferFreq.toInt()} Hz (24dB/oct)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        // Bass Cutoff and Volume Controls
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Acoustic Subwoofer Cabinet Tuning",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Cutoff Frequency
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Low-Frequency Cutoff Crossover", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${subwooferFreq.toInt()} Hz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubCyan)
                    }
                    Slider(
                        value = subwooferFreq,
                        onValueChange = { viewModel.setSubwooferFrequency(it) },
                        valueRange = 20.0f..150.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = SubCyan,
                            activeTrackColor = SubCyan,
                            inactiveTrackColor = SpaceBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crossover_frequency_slider")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Multiplier
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Bass Harmonic Multiplier", fontSize = 13.sp, color = TextSecondary)
                        Text(text = String.format("%.1fx Boost", bassBoost), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BassPurple)
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = { viewModel.setBassBoost(it) },
                        valueRange = 1.0f..4.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = BassPurple,
                            activeTrackColor = BassPurple,
                            inactiveTrackColor = SpaceBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bass_multiplier_slider")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phase inversion toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.togglePhaseInversion() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Acoustic Phase Inversion (180°)", fontSize = 14.sp, color = TextPrimary)
                        Text(text = "Prevents cancellation with satellite speakers", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = phaseInversion,
                        onCheckedChange = { viewModel.togglePhaseInversion() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SubCyan,
                            checkedTrackColor = SubCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = SlateLight,
                            uncheckedTrackColor = SpaceBlack
                        ),
                        modifier = Modifier.testTag("phase_inversion_switch")
                    )
                }
            }
        }

        // Discrete Master Tone & Gain Controllers
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "High-Fidelity Tone & Gain",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // Treble Knob
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Treble Shimmer Accent", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${(trebleVol * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HiResGold)
                    }
                    Slider(
                        value = trebleVol,
                        onValueChange = { viewModel.setTrebleVolume(it) },
                        valueRange = 0.0f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = HiResGold,
                            activeTrackColor = HiResGold,
                            inactiveTrackColor = SpaceBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("treble_slider")
                    )
                }

                // Mid Clarity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Vocal Midrange Focus", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${(midVol * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Slider(
                        value = midVol,
                        onValueChange = { viewModel.setMidVolume(it) },
                        valueRange = 0.0f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = TextPrimary,
                            inactiveTrackColor = SpaceBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mid_slider")
                    )
                }

                // Master Gain / Limiter
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "DSP Master Output Gain", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${(masterGain * 100).toInt()}%", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (masterGain > 1.1f) ErrorRed else SubCyan
                        )
                    }
                    Slider(
                        value = masterGain,
                        onValueChange = { viewModel.setMasterGain(it) },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = SubCyan,
                            activeTrackColor = SubCyan,
                            inactiveTrackColor = SpaceBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_gain_slider")
                    )
                    if (masterGain > 1.1f) {
                        Text(
                            text = "Warning: Master gain entering soft-saturation clipping threshold.",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// 3. EQUALIZER TAB
// ==========================================
@Composable
fun EqualizerTab(viewModel: PlayerViewModel) {
    val eqBands by viewModel.eqBands.collectAsState()
    val activePreset by viewModel.selectedPresetName.collectAsState()

    val presets = listOf(
        "Audiophile Flat",
        "Subwoofer Heavy",
        "Acoustic Live",
        "Dolby Theatre",
        "Vocal Clarity",
        "Electro Synth"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Curve Visualizer card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, SlateLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "10-BAND GRAPHIC EQUALIZER SPLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HiResGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Graphic curve path drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(SpaceBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateMedium, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Draw grid lines
                            drawLine(color = SlateMedium, start = Offset(0f, h / 2), end = Offset(w, h / 2), strokeWidth = 1f)
                            drawLine(color = SlateMedium.copy(alpha = 0.5f), start = Offset(0f, h * 0.25f), end = Offset(w, h * 0.25f), strokeWidth = 0.5f)
                            drawLine(color = SlateMedium.copy(alpha = 0.5f), start = Offset(0f, h * 0.75f), end = Offset(w, h * 0.75f), strokeWidth = 0.5f)

                            val step = w / (eqBands.size - 1)
                            val path = Path()

                            eqBands.forEachIndexed { i, bandGain ->
                                // Convert 0.0f..2.5f gain range to pixel coords. 1.0f (flat) is center.
                                // Invert: 2.5 is top (gain), 0.0 is bottom (attenuation)
                                val gainY = h - ((bandGain / 2.5f) * h)
                                val x = i * step
                                if (i == 0) {
                                    path.moveTo(x, gainY)
                                } else {
                                    val prevX = (i - 1) * step
                                    val prevGainY = h - ((eqBands[i - 1] / 2.5f) * h)
                                    // Smooth bezier spline connecting points
                                    path.cubicTo(
                                        prevX + step / 2f, prevGainY,
                                        x - step / 2f, gainY,
                                        x, gainY
                                    )
                                }
                            }

                            // Draw glowing path
                            drawPath(
                                path = path,
                                color = HiResGold,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }

        // Quick Preset Selector Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Acoustic Presets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isSelected = activePreset == preset
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) HiResGold else SlateDark,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) HiResGold else SlateLight,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.applyPreset(preset) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("eq_preset_${preset.replace(" ", "_").lowercase()}")
                        ) {
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SpaceBlack else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 10 Band Gain Sliders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Fidelity Precision Frequency Dials",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val freqLabels = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

                eqBands.forEachIndexed { index, gain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabels[index],
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.width(50.dp)
                        )
                        Slider(
                            value = gain,
                            onValueChange = { viewModel.updateEqBand(index, it) },
                            valueRange = 0.0f..2.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = HiResGold,
                                activeTrackColor = HiResGold,
                                inactiveTrackColor = SpaceBlack
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eq_slider_$index")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Convert gain to dB readout representation:
                        // 1.0f -> 0dB, 2.5f -> +12dB, 0.0f -> -24dB
                        val dbVal = if (gain >= 1.0f) {
                            (gain - 1.0f) * 8.0f
                        } else {
                            (gain - 1.0f) * 24.0f
                        }
                        
                        Text(
                            text = if (Math.abs(dbVal) < 0.1f) "Flat" else String.format("%+.1f dB", dbVal),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (dbVal > 0.1f) HiResGold else TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(55.dp)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// 4. ATMOS & CODEC TAB
// ==========================================
@Composable
fun AtmosCodecTab(viewModel: PlayerViewModel) {
    val dolbyDts by viewModel.dolbyDtsEnabled.collectAsState()
    val environment by viewModel.spatialEnvironment.collectAsState()
    val panX by viewModel.panX.collectAsState()
    val panY by viewModel.panY.collectAsState()
    val activeCodec by viewModel.codecMode.collectAsState()

    val environments = listOf("Studio", "Concert Hall", "Cathedral", "Cinema")
    val codecs = listOf("SBC", "AAC", "aptX HD", "LDAC")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dolby DTS Atmos Immersion controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, SubCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ATMOS SOUND FIELD MATRIX",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SubCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Dolby DTS Spatializer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Switch(
                            checked = dolbyDts,
                            onCheckedChange = { viewModel.toggleDolbyDts() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SubCyan,
                                checkedTrackColor = SubCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = SlateLight,
                                uncheckedTrackColor = SpaceBlack
                            ),
                            modifier = Modifier.testTag("dolby_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Calculates multi-directional ear canal transfers (HRTF) to expand speaker depth beyond regular stereo thresholds.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // 3D Spatial Audio Joypad and Environment Caster
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "3D Acoustic Coordinate Locator",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Drag the node to position the spatial audio origin",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // The 2D Joypad Canvas for dynamic coordinate tracking
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(SpaceBlack, RoundedCornerShape(12.dp))
                        .border(1.dp, SlateLight, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Draw coordinate plane
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        // Radar circle rings
                        drawCircle(color = SlateMedium, radius = h / 2f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1f))
                        drawCircle(color = SlateMedium.copy(alpha = 0.5f), radius = h / 3f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1f))
                        drawCircle(color = SlateMedium.copy(alpha = 0.25f), radius = h / 5f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1f))

                        // Crosshairs
                        drawLine(color = SlateMedium, start = Offset(0f, h / 2f), end = Offset(w, h / 2f))
                        drawLine(color = SlateMedium, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h))
                    }

                    // Touch Interactive Handle
                    var containerWidth by remember { mutableStateOf(1) }
                    var containerHeight by remember { mutableStateOf(1) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Scale to -1.0f..1.0f
                                    val nextX = (panX + (dragAmount.x / (size.width / 2f))).coerceIn(-1.0f, 1.0f)
                                    val nextY = (panY + (dragAmount.y / (size.height / 2f))).coerceIn(0.0f, 1.0f)
                                    viewModel.set3DPosition(nextX, nextY)
                                }
                            }
                    ) {
                        // Labels
                        Text("LEFT", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart).padding(8.dp))
                        Text("RIGHT", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp))
                        Text("DRY FRONT", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
                        Text("WET REVERB REAR", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))

                        // Pulsating handle representing Sound Source
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val wPx = constraints.maxWidth.toFloat()
                            val hPx = constraints.maxHeight.toFloat()
                            
                            // Map -1.0f..1.0f to coordinate space
                            val handleX = ((panX + 1.0f) / 2.0f) * wPx
                            val handleY = panY * hPx

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (handleX - 16.dp.toPx()).toInt(),
                                            (handleY - 16.dp.toPx()).toInt()
                                        )
                                    }
                                    .size(32.dp)
                                    .background(SubCyan, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .shadow(6.dp, CircleShape)
                                    .testTag("spatial_audio_joypad")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Environment selector
                Text(
                    text = "Acoustic Reflection Chamber",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    environments.forEach { env ->
                        val isSelected = environment == env
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) SubCyan else SpaceBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) SubCyan else SlateMedium, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSpatialEnvironment(env) }
                                .padding(vertical = 10.dp)
                                .testTag("env_${env.replace(" ", "_").lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = env,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SpaceBlack else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Bluetooth Codec Switcher panel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Audiophile Bluetooth Codec Switcher",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Simulates how codec compression alters acoustic transparency",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    codecs.forEach { codec ->
                        val isSelected = activeCodec == codec
                        val color = when (codec) {
                            "LDAC" -> HiResGold
                            "aptX HD" -> SubCyan
                            "AAC" -> TextPrimary
                            else -> TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) color else SpaceBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) color else SlateMedium, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setCodecMode(codec) }
                                .padding(vertical = 12.dp)
                                .testTag("codec_mode_$codec"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = codec,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SpaceBlack else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostic / Technical Comparison Card
                val specs = getCodecSpecs(activeCodec)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpaceBlack),
                    border = BorderStroke(1.dp, SlateLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Codec Specification:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = specs.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = specs.color)
                        }
                        Divider(color = SlateMedium, modifier = Modifier.padding(vertical = 6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Active Bitrate:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = specs.bitrate, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sample Resolution:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = specs.resolution, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Simulated Latency:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = specs.latency, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = specs.latencyColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Spectrum Purity Index:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = specs.purity, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = specs.color)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = specs.explanation,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// UTILITY / HELPERS / SUBCOMPOSABLES
// ==========================================

data class CodecSpec(
    val name: String,
    val bitrate: String,
    val resolution: String,
    val latency: String,
    val latencyColor: Color,
    val purity: String,
    val explanation: String,
    val color: Color
)

fun getCodecSpecs(codec: String): CodecSpec {
    return when (codec) {
        "LDAC" -> CodecSpec(
            name = "Sony LDAC™ (Lossless Mode)",
            bitrate = "990 kbps (High Resolution)",
            resolution = "96 kHz / 24-bit PCM",
            latency = "30 ms (Ultra Low Mode)",
            latencyColor = Color(0xFF00FF88),
            purity = "99.8% (Master Transparency)",
            explanation = "Delivers true high-resolution linear acoustics with zero quantizing degradation. Perfect for testing physical subwoofer setups.",
            color = HiResGold
        )
        "aptX HD" -> CodecSpec(
            name = "Qualcomm aptX™ HD",
            bitrate = "576 kbps (Primacy Mode)",
            resolution = "48 kHz / 24-bit LPCM",
            latency = "45 ms (Balanced)",
            latencyColor = SubCyan,
            purity = "92.0% (Near-Lossless Highs)",
            explanation = "Maintains a pristine 24-bit bitdepth using adaptive compression. Warm signature, great for mid and vocal ranges.",
            color = SubCyan
        )
        "AAC" -> CodecSpec(
            name = "Apple AAC (MPEG-4 Layer)",
            bitrate = "256 kbps (Standard Mode)",
            resolution = "44.1 kHz / 16-bit Stereo",
            latency = "110 ms (High Latency)",
            latencyColor = HiResGold,
            purity = "81.0% (Warm / Psychoacoustic)",
            explanation = "Uses psychoacoustic models to strip imperceptible high frequencies. Adds slight warmth in high mids, mild bass rolloff.",
            color = TextPrimary
        )
        else -> CodecSpec(
            name = "Standard Subband Codec (SBC)",
            bitrate = "128 kbps (Degraded / Fallback)",
            resolution = "44.1 kHz / 8-bit Quantized",
            latency = "170 ms (Extreme Latency)",
            latencyColor = ErrorRed,
            purity = "42.0% (Heavy Digital Grit)",
            explanation = "Slashes bandwidth, quantizing the signal to a coarse bit depth. This generates audible digital grain, rendering subwoofers distorted.",
            color = ErrorRed
        )
    }
}

@Composable
fun FormatBadge(track: TrackInfo) {
    Box(
        modifier = Modifier
            .background(HiResGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, HiResGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HIGH-RES AUDIO",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = HiResGold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${track.format} • ${track.bitDepth}/${track.sampleRate}",
                fontSize = 8.sp,
                color = HiResGold,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlaybackControlsRow(viewModel: PlayerViewModel, isCompact: Boolean) {
    val isPlaying by viewModel.isPlaying.collectAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { viewModel.prevTrack() },
            modifier = Modifier
                .size(if (isCompact) 40.dp else 52.dp)
                .background(SlateMedium, CircleShape)
                .testTag("prev_track_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Track",
                tint = TextPrimary,
                modifier = Modifier.size(if (isCompact) 20.dp else 28.dp)
            )
        }

        IconButton(
            onClick = { viewModel.togglePlayback() },
            modifier = Modifier
                .size(if (isCompact) 56.dp else 72.dp)
                .background(
                    Brush.radialGradient(listOf(BassPurple, SpaceBlack)),
                    CircleShape
                )
                .border(2.dp, SubCyan, CircleShape)
                .shadow(8.dp, CircleShape)
                .testTag("play_pause_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = SubCyan,
                modifier = Modifier.size(if (isCompact) 28.dp else 36.dp)
            )
        }

        IconButton(
            onClick = { viewModel.nextTrack() },
            modifier = Modifier
                .size(if (isCompact) 40.dp else 52.dp)
                .background(SlateMedium, CircleShape)
                .testTag("next_track_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Track",
                tint = TextPrimary,
                modifier = Modifier.size(if (isCompact) 20.dp else 28.dp)
            )
        }
    }
}

@Composable
fun FullSubwooferVisualizer(viewModel: PlayerViewModel) {
    val subAmplitude by viewModel.subwooferAmplitude.collectAsState()
    val rawWaveform by viewModel.waveformBuffer.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Slow rotation for the outer audio wheel ring
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Glowing pulsator animation based on subwoofer volume
    val subScale = 1.0f + (subAmplitude * 0.35f)

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw real-time surrounding waveform rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val baseRadius = 80.dp.toPx()

            // 1. Draw glowing soundwave nodes radiating from the center ring
            val path = Path()
            val pointsCount = rawWaveform.size
            
            for (i in 0 until pointsCount) {
                val waveVal = if (isPlaying) rawWaveform[i] else 0.0f
                val nodeAngle = (i.toFloat() / pointsCount.toFloat()) * 2.0f * Math.PI.toFloat() + (angle * Math.PI.toFloat() / 180f)
                
                // Modulate the radius with the waveform sample
                val r = baseRadius + (waveVal * 45.dp.toPx())
                val x = center.x + r * cos(nodeAngle)
                val y = center.y + r * sin(nodeAngle)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // Outer laser audio ring
            drawPath(
                path = path,
                brush = Brush.sweepGradient(listOf(BassPurple, SubCyan, BassPurple)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic Subwoofer Crossover background circles
            drawCircle(
                color = BassPurple.copy(alpha = (subAmplitude * 0.3f).coerceIn(0.08f, 0.5f)),
                radius = (baseRadius - 10.dp.toPx()) * subScale,
                center = center
            )
        }

        // Inner Subwoofer Physical Cone
        Box(
            modifier = Modifier
                .size((110.dp * subScale).coerceIn(90.dp, 160.dp))
                .background(SlateMedium, CircleShape)
                .border(3.dp, SubCyan, CircleShape)
                .shadow(12.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cyberpunk_album),
                contentDescription = "Album Art Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            // Shadow Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            )

            // Metallic center speaker dust cap
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), SpaceBlack)),
                        CircleShape
                    )
                    .border(1.dp, SubCyan.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}

@Composable
fun CompactSubwooferVisualizer(viewModel: PlayerViewModel) {
    val subAmplitude by viewModel.subwooferAmplitude.collectAsState()
    val subScale = 1.0f + (subAmplitude * 0.3f)

    Box(
        modifier = Modifier
            .size(130.dp)
            .background(SlateMedium, CircleShape)
            .border(2.dp, BassPurple, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SubCyan.copy(alpha = (subAmplitude * 0.4f).coerceIn(0.1f, 0.6f)),
                radius = (50.dp.toPx()) * subScale,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Image(
            painter = painterResource(id = R.drawable.img_cyberpunk_album),
            contentDescription = "Art Mini",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
