package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChannelItem
import com.example.data.IptvRepository
import com.example.data.MediaType
import com.example.data.PlayableMedia
import com.example.ui.theme.IptvCardBackground
import com.example.ui.theme.IptvCardBorder
import com.example.ui.theme.IptvTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-App Video Player Screen
 * Seamless playback for Live TV Channels, VOD Movies, and Radio Stations:
 * 1. Full-screen canvas with dark gradient overlays
 * 2. Top-Left Header: Custom channel/movie logo via Coil AsyncImage, title & category
 * 3. Top-Right Info: Real-time clock (12:51) + Cloud weather icon + 24°
 * 4. Center: Video stream or Radio audio waveform visualizer
 * 5. Bottom Control Bar:
 *    - Left: Live/VOD/Radio badge, Title, Subtitle, running timestamp
 *    - Center: Previous, large frosted Play/Pause toggle, Next, and centered downward chevron
 *    - Right: Subtitles toggle, Favorite star, Settings gear modal
 */
@Composable
fun VideoPlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by IptvRepository.channels.collectAsState()
    val movies by IptvRepository.movies.collectAsState()
    val radios by IptvRepository.radios.collectAsState()
    val activeMedia by IptvRepository.currentPlayingMedia.collectAsState()

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showMediaGuide by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var currentAspectRatio by remember { mutableStateOf("16:9") }
    var currentAudioTrack by remember { mutableStateOf("Dolby Surround 5.1") }

    // Live playback timer: starts around 00:42:27 and increments
    var playbackSeconds by remember { mutableIntStateOf(42 * 60 + 27) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000L)
            playbackSeconds++
        }
    }

    val formattedDuration = remember(playbackSeconds) {
        val hours = playbackSeconds / 3600
        val minutes = (playbackSeconds % 3600) / 60
        val seconds = playbackSeconds % 60
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Top-right synchronized real-time clock
    var currentTimeString by remember { mutableStateOf("12:51") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30_000L)
        }
    }

    // Auto-hide controls after 6 seconds of inactivity
    LaunchedEffect(showControls, showSettingsModal, showMediaGuide) {
        if (showControls && !showSettingsModal && !showMediaGuide) {
            delay(6000L)
            showControls = false
        }
    }

    val media = activeMedia ?: PlayableMedia(
        id = "default",
        title = stringResource(R.string.player_channel_default),
        subtitle = stringResource(R.string.player_category_default),
        streamUrl = "",
        logoUrl = "https://images.unsplash.com/photo-1561731216-c3a4d99437d5?w=500&q=80",
        mediaType = MediaType.LIVE_TV,
        isFavorite = true
    )

    val isFavorite = media.isFavorite

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    showControls = !showControls
                }
            }
            .testTag("video_player_screen")
    ) {
        // ========================================================
        // 1. FULL SCREEN VIDEO STREAM CANVAS
        // ========================================================
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (media.mediaType == MediaType.RADIO) {
                // Radio Mode: Station backdrop with Audio Waveform Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF151922), Color(0xFF0D1017), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Custom Station Logo
                        AsyncImage(
                            model = media.logoUrl,
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(3.dp, Color(0xFF4FC3F7)), CircleShape)
                        )

                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = media.subtitle,
                            color = Color(0xFF81D4FA),
                            fontSize = 15.sp
                        )

                        // Equalizer Waveform Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(20, 36, 16, 44, 28, 50, 22, 40, 18, 32).forEach { heightDp ->
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(if (isPlaying) heightDp.dp else 8.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF4FC3F7), Color(0xFF2979FF))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                // Video Mode (Live TV or Movie)
                Image(
                    painter = painterResource(id = R.drawable.bg_leopard_stream),
                    contentDescription = "Live Video Stream",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video_player_surface")
                )
            }

            // Dynamic Dark Gradient Overlays for high contrast
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }
        }

        // ========================================================
        // 2. TOP OVERLAY BAR: Custom Logo & Time/Weather
        // ========================================================
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .testTag("player_top_overlay"),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-Left: Back button + Dynamic Custom Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Dynamic Custom Logo Header
                    MediaLogoHeader(
                        media = media,
                        onClick = { showMediaGuide = !showMediaGuide }
                    )
                }

                // Top-Right: Synchronized Time & Weather ("12:51  ☁ 24°")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("player_top_weather_time")
                ) {
                    Text(
                        text = currentTimeString,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )

                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Weather",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "24°",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ========================================================
        // 3. BOTTOM OVERLAY BAR: Controls, Live Badge, Actions
        // ========================================================
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .testTag("player_bottom_controls"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // --- LEFT SECTION: Badge, Title, Subtitle, Running Time ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_left_info"),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Dynamic Type Badge
                        when (media.mediaType) {
                            MediaType.LIVE_TV -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE50914))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("player_live_badge")
                                ) {
                                    Text(
                                        text = stringResource(R.string.player_live),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            MediaType.MOVIE -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00897B))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("player_live_badge")
                                ) {
                                    Text(
                                        text = "4K VOD MOVIE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            MediaType.RADIO -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7B1FA2))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("player_live_badge")
                                ) {
                                    Text(
                                        text = "LIVE RADIO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Program / Media Title
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )

                        // Subtitle / Channel
                        Text(
                            text = media.subtitle,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Running Timestamp (e.g. "00:42:27")
                        Text(
                            text = formattedDuration,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.testTag("player_duration_text")
                        )
                    }

                    // --- CENTER SECTION: Previous, Large Frosted Play/Pause, Next & Down Chevron ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.testTag("player_center_controls")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            // Previous Media Button
                            IconButton(
                                onClick = {
                                    when (media.mediaType) {
                                        MediaType.LIVE_TV -> {
                                            val idx = channels.indexOfFirst { it.id == media.id }
                                            if (idx > 0) IptvRepository.playChannel(channels[idx - 1])
                                            else if (channels.isNotEmpty()) IptvRepository.playChannel(channels.last())
                                        }
                                        MediaType.MOVIE -> {
                                            val idx = movies.indexOfFirst { it.id == media.id }
                                            if (idx > 0) IptvRepository.playMovie(movies[idx - 1])
                                            else if (movies.isNotEmpty()) IptvRepository.playMovie(movies.last())
                                        }
                                        MediaType.RADIO -> {
                                            val idx = radios.indexOfFirst { it.id == media.id }
                                            if (idx > 0) IptvRepository.playRadio(radios[idx - 1])
                                            else if (radios.isNotEmpty()) IptvRepository.playRadio(radios.last())
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("player_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Distinct Large Frosted Circular Play / Pause Toggle
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.28f))
                                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.55f)), CircleShape)
                                    .clickable { isPlaying = !isPlaying }
                                    .testTag("player_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Next Media Button
                            IconButton(
                                onClick = {
                                    when (media.mediaType) {
                                        MediaType.LIVE_TV -> {
                                            val idx = channels.indexOfFirst { it.id == media.id }
                                            if (idx >= 0 && idx < channels.lastIndex) IptvRepository.playChannel(channels[idx + 1])
                                            else if (channels.isNotEmpty()) IptvRepository.playChannel(channels.first())
                                        }
                                        MediaType.MOVIE -> {
                                            val idx = movies.indexOfFirst { it.id == media.id }
                                            if (idx >= 0 && idx < movies.lastIndex) IptvRepository.playMovie(movies[idx + 1])
                                            else if (movies.isNotEmpty()) IptvRepository.playMovie(movies.first())
                                        }
                                        MediaType.RADIO -> {
                                            val idx = radios.indexOfFirst { it.id == media.id }
                                            if (idx >= 0 && idx < radios.lastIndex) IptvRepository.playRadio(radios[idx + 1])
                                            else if (radios.isNotEmpty()) IptvRepository.playRadio(radios.first())
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("player_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Centered Small Down-Arrow (Hide overlay controls)
                        IconButton(
                            onClick = { showControls = false },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("player_hide_controls_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Hide controls",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // --- RIGHT SECTION: Subtitles, Favorite Star, Settings Gear ---
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_right_actions"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { subtitlesEnabled = !subtitlesEnabled },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("player_subtitles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (subtitlesEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { IptvRepository.toggleFavorite(media.id) },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("player_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF9500) else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { showSettingsModal = !showSettingsModal },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("player_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Playback Settings",
                                tint = if (showSettingsModal) Color(0xFF4FC3F7) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // 4. QUICK SETTINGS MODAL / POPUP
        // ========================================================
        if (showSettingsModal) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 100.dp)
                    .width(290.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEE1E2430),
                border = BorderStroke(1.dp, Color(0xFF353E4F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playback Options",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Audio Track", color = IptvTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = currentAudioTrack, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Aspect Ratio", color = IptvTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16:9", "4:3", "Fill").forEach { ratio ->
                            val isSelected = currentAspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF4FC3F7) else Color(0xFF2A3342))
                                    .clickable { currentAspectRatio = ratio }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = ratio,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quality: ${media.qualityOrDuration}",
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ========================================================
        // 5. SIDEBAR GUIDE: Quick Switcher for Channels / Movies / Radios
        // ========================================================
        AnimatedVisibility(
            visible = showMediaGuide,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .padding(top = 80.dp, bottom = 90.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = IptvCardBackground,
                border = BorderStroke(1.dp, IptvCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Channel & Media Guide",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showMediaGuide = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Close Guide",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(channels) { channel ->
                            val isCurrent = channel.id == media.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0xFF2C3545) else Color(0xFF1E2430))
                                    .border(
                                        BorderStroke(1.dp, if (isCurrent) Color(0xFF4FC3F7) else Color.Transparent),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        IptvRepository.playChannel(channel)
                                        showMediaGuide = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Custom Channel Logo loaded dynamically
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = channel.currentProgram,
                                            color = IptvTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }

                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top-left Logo Header: Dynamically renders custom logo from Admin Panel URL
 * or iconic Nat Geo frame for Nat Geo Wild
 */
@Composable
private fun MediaLogoHeader(
    media: PlayableMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (media.title.contains("Nat Geo", ignoreCase = true)) {
            // Yellow National Geographic Frame
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(24.dp)
                    .border(BorderStroke(2.5.dp, Color(0xFFFFCC00)), RoundedCornerShape(1.dp))
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "NAT GEO",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    lineHeight = 11.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WILD",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 2.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "HD",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else {
            // Dynamic Custom Logo from Admin Panel URL
            if (!media.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = media.logoUrl,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, Color(0x66FFFFFF)), RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF242C3C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (media.mediaType) {
                            MediaType.LIVE_TV -> Icons.Default.Tv
                            MediaType.MOVIE -> Icons.Default.Movie
                            MediaType.RADIO -> Icons.Default.Radio
                        },
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = media.subtitle,
                    color = IptvTextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
