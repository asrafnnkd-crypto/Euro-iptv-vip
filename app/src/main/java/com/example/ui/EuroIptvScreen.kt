package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppLanguage
import com.example.data.ChannelItem
import com.example.data.IptvRepository
import com.example.data.MediaType
import com.example.data.MovieItem
import com.example.data.RadioStation
import com.example.ui.theme.IptvAccentOrange
import com.example.ui.theme.IptvBackground
import com.example.ui.theme.IptvBackgroundDark
import com.example.ui.theme.IptvCardBackground
import com.example.ui.theme.IptvCardBorder
import com.example.ui.theme.IptvCardBorderFocused
import com.example.ui.theme.IptvCardFocused
import com.example.ui.theme.IptvHeaderElementBg
import com.example.ui.theme.IptvProfileRed
import com.example.ui.theme.IptvTextMuted
import com.example.ui.theme.IptvTextPrimary
import com.example.ui.theme.IptvTextSecondary
import com.example.ui.theme.IptvUnderlineGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pixel-Perfect Android IPTV User Interface
 * Faithfully replicates the dark-mode smart TV dashboard design from reference screenshots.
 * Integrates dynamic content fetching from remote Admin Panel / data source with custom logos.
 */
@Composable
fun EuroIptvScreen(
    onOpenPlayer: (String) -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic Content State from IptvRepository
    val channels by IptvRepository.channels.collectAsState()
    val movies by IptvRepository.movies.collectAsState()
    val radios by IptvRepository.radios.collectAsState()
    val adminConfig by IptvRepository.adminConfig.collectAsState()

    // Selected Card Index: 0 = Live TV's (Default focused), 1 = Movies, 2 = Radios
    var selectedCardIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshingRemote by remember { mutableStateOf(false) }

    // Dynamic Catalog Drawer / Sheet (Live TV, Movies, or Radios)
    var activeDrawerCategory by remember { mutableStateOf<MediaType?>(null) }
    var showChannelPreviewSheet by remember { mutableStateOf(false) }

    // Current selected app language
    val currentLanguage by IptvRepository.currentLanguage.collectAsState()

    // Live real-time clock updating every 30 seconds
    var currentTimeString by remember { mutableStateOf("12:51") }
    var currentDateString by remember { mutableStateOf("Wednesday, Jan 2") }

    LaunchedEffect(currentLanguage) {
        val locale = if (currentLanguage == AppLanguage.ARABIC) Locale("ar") else Locale.ENGLISH
        while (true) {
            val now = Date()
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDateString = SimpleDateFormat("EEEE, d MMMM", locale).format(now)
            delay(30_000L)
        }
    }

    // Dynamic Remote Content Initialization: fetch from remote endpoint on entry
    LaunchedEffect(Unit) {
        IptvRepository.fetchRemoteContent(adminConfig.serverUrl)
    }

    fun syncRemoteData() {
        if (isRefreshingRemote) return
        isRefreshingRemote = true
        coroutineScope.launch {
            val success = IptvRepository.fetchRemoteContent(adminConfig.serverUrl)
            isRefreshingRemote = false
            if (success) {
                Toast.makeText(context, "Dynamic content & custom logos synced!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Channels refreshed from server cache", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF242A35),
                        IptvBackground,
                        IptvBackgroundDark
                    ),
                    radius = 1600f
                )
            )
            .testTag("euro_iptv_screen")
    ) {
        // Subtle background ambient illumination
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1A4F6B94),
                            Color.Transparent,
                            Color(0x330E1117)
                        )
                    )
                )
        )

        // Main Responsive Container
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscapeOrWide = maxWidth >= 700.dp
            val horizontalPadding = if (isLandscapeOrWide) 48.dp else 20.dp
            val verticalPadding = if (isLandscapeOrWide) 32.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ========================================================
                // 1. TOP NAVIGATION BAR (HEADER)
                // ========================================================
                TopNavigationBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    isRefreshing = isRefreshingRemote,
                    onSyncClick = { syncRemoteData() },
                    onVoiceSearchClick = {
                        Toast.makeText(context, "Listening for channel or movie title...", Toast.LENGTH_SHORT).show()
                    },
                    onSettingsClick = { onOpenAdminPanel() },
                    onProfileClick = { onOpenAdminPanel() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ========================================================
                // 2. MAIN CONTENT AREA (CARDS GRID)
                // ========================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    MainCardsRow(
                        selectedIndex = selectedCardIndex,
                        channelsCount = "${channels.size} Channels",
                        moviesCount = "${movies.size} Movies",
                        radiosCount = "${radios.size} Stations",
                        onCardSelected = { index ->
                            selectedCardIndex = index
                            when (index) {
                                0 -> activeDrawerCategory = MediaType.LIVE_TV
                                1 -> activeDrawerCategory = MediaType.MOVIE
                                2 -> activeDrawerCategory = MediaType.RADIO
                            }
                        },
                        onPlayDirect = { index ->
                            when (index) {
                                0 -> {
                                    val ch = channels.firstOrNull()
                                    if (ch != null) IptvRepository.playChannel(ch)
                                    onOpenPlayer("live")
                                }
                                1 -> {
                                    val mov = movies.firstOrNull()
                                    if (mov != null) IptvRepository.playMovie(mov)
                                    onOpenPlayer("movies")
                                }
                                2 -> {
                                    val rad = radios.firstOrNull()
                                    if (rad != null) IptvRepository.playRadio(rad)
                                    onOpenPlayer("radios")
                                }
                            }
                        }
                    )
                }

                // ========================================================
                // 3. BOTTOM WIDGET AREA (TIME, DATE, WEATHER)
                // ========================================================
                BottomInfoWidget(
                    timeString = currentTimeString,
                    dateString = currentDateString,
                    temperature = "24°",
                    onPreviewClick = { showChannelPreviewSheet = !showChannelPreviewSheet }
                )
            }
        }

        // ========================================================
        // 4. DYNAMIC CONTENT DRAWER / CATALOG (Live TV, Movies, Radios)
        // Displays dynamically fetched items with their custom logos
        // ========================================================
        AnimatedVisibility(
            visible = activeDrawerCategory != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            activeDrawerCategory?.let { categoryType ->
                DynamicMediaCatalogDrawer(
                    mediaType = categoryType,
                    channels = channels,
                    movies = movies,
                    radios = radios,
                    onSelectChannel = { channel ->
                        IptvRepository.playChannel(channel)
                        onOpenPlayer("live")
                    },
                    onSelectMovie = { movie ->
                        IptvRepository.playMovie(movie)
                        onOpenPlayer("movies")
                    },
                    onSelectRadio = { radio ->
                        IptvRepository.playRadio(radio)
                        onOpenPlayer("radios")
                    },
                    onClose = { activeDrawerCategory = null }
                )
            }
        }

        // Interactive Channel Preview Overlay (from reference screenshot)
        AnimatedVisibility(
            visible = showChannelPreviewSheet && activeDrawerCategory == null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            ChannelQuickPreviewOverlay(
                channels = channels,
                onSelectChannel = { channel ->
                    IptvRepository.playChannel(channel)
                    onOpenPlayer("live")
                },
                onClose = { showChannelPreviewSheet = false }
            )
        }
    }
}

/**
 * Top Navigation Bar with Logo, Voice Search, Search Field, Cloud Remote Sync, Settings & Profile.
 */
@Composable
private fun TopNavigationBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isRefreshing: Boolean,
    onSyncClick: () -> Unit,
    onVoiceSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_navigation_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Left: Brand Logo ("EURO IPTV") + Voice Mic Button ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // EURO IPTV Typography
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("app_logo")
            ) {
                Text(
                    text = "EURO",
                    color = IptvTextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "IPTV",
                    color = IptvTextSecondary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.8.sp
                )
            }

            // Vertical subtle separator
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .width(1.dp)
                    .background(Color(0xFF333B4A))
            )

            // Voice Search Microphone Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(IptvHeaderElementBg)
                    .clickable(onClick = onVoiceSearchClick)
                    .testTag("voice_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = IptvTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Remote Cloud Sync Button (Dynamic endpoint refresh)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(IptvHeaderElementBg)
                    .clickable(onClick = onSyncClick)
                    .testTag("remote_sync_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF4FC3F7),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Sync Remote Endpoint",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- Center: Search Field ("Search here") ---
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(IptvHeaderElementBg)
                .border(BorderStroke(1.dp, Color(0xFF2F3847)), RoundedCornerShape(21.dp))
                .padding(horizontal = 16.dp)
                .testTag("search_container"),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = IptvTextSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = IptvTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(IptvTextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_text_field"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                color = IptvTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // --- Right: Language Selector + Settings + Profile Avatar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Arabic / English Language Switcher
            LanguageSelectorWidget()

            // Settings Button (Direct jump to Remote Admin Panel)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(IptvHeaderElementBg)
                    .clickable(onClick = onSettingsClick)
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings & Remote Admin",
                    tint = IptvTextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            // User Profile Red Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(IptvProfileRed)
                    .clickable(onClick = onProfileClick)
                    .testTag("profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Top Bar Language Selector Dropdown
 */
@Composable
private fun LanguageSelectorWidget() {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage by IptvRepository.currentLanguage.collectAsState()

    Box {
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(IptvHeaderElementBg)
                .border(BorderStroke(1.dp, Color(0xFF2F3847)), RoundedCornerShape(21.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp)
                .testTag("language_selector_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = currentLanguage.displayName,
                color = IptvTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = IptvTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(IptvCardBackground)
                .border(BorderStroke(1.dp, IptvCardBorder), RoundedCornerShape(12.dp))
                .testTag("language_dropdown_menu")
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) Color(0xFF4FC3F7) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4FC3F7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        IptvRepository.setLanguage(lang)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = Color.White),
                    modifier = Modifier.testTag("language_option_${lang.code}")
                )
            }
        }
    }
}

/**
 * Three Main Vertical Cards in a Horizontal Arrangement:
 * 1. Live TV's (Selected/Focused with white underline)
 * 2. Movies (with "New" orange pill badge)
 * 3. Radios (with artist background silhouette and "Playing..." status)
 */
@Composable
private fun MainCardsRow(
    selectedIndex: Int,
    channelsCount: String,
    moviesCount: String,
    radiosCount: String,
    onCardSelected: (Int) -> Unit,
    onPlayDirect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ----------------------------------------------------
        // Card 1: LIVE TV'S (FOCUSED / SELECTED STATE)
        // ----------------------------------------------------
        IptvContentCard(
            title = stringResource(R.string.live_tvs),
            subtitle = channelsCount,
            iconRes = R.drawable.ic_iptv_live_tv,
            isSelected = selectedIndex == 0,
            onClick = { onCardSelected(0) },
            onDoubleClick = { onPlayDirect(0) },
            modifier = Modifier.testTag("card_live_tvs"),
            topContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sync_update),
                        contentDescription = "Update Status",
                        tint = IptvTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.last_update),
                        color = IptvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        )

        // ----------------------------------------------------
        // Card 2: MOVIES (WITH "NEW" BADGE)
        // ----------------------------------------------------
        IptvContentCard(
            title = stringResource(R.string.movies),
            subtitle = moviesCount,
            iconRes = R.drawable.ic_iptv_movies,
            isSelected = selectedIndex == 1,
            onClick = { onCardSelected(1) },
            onDoubleClick = { onPlayDirect(1) },
            modifier = Modifier.testTag("card_movies"),
            topContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IptvAccentOrange)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_new),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        )

        // ----------------------------------------------------
        // Card 3: RADIOS (WITH ARTIST SILHOUETTE & PLAYING STATUS)
        // ----------------------------------------------------
        IptvContentCard(
            title = stringResource(R.string.radios),
            subtitle = radiosCount,
            iconRes = R.drawable.ic_iptv_radios,
            isSelected = selectedIndex == 2,
            onClick = { onCardSelected(2) },
            onDoubleClick = { onPlayDirect(2) },
            backgroundImageRes = R.drawable.img_radio_artist,
            modifier = Modifier.testTag("card_radios"),
            topContent = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x991B202A))
                        .border(BorderStroke(0.8.dp, Color(0x33FFFFFF)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = stringResource(R.string.playing_radio),
                            color = Color(0xFFD8DFEB),
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        )
    }
}

/**
 * Reusable Card component for Live TV, Movies, and Radios.
 */
@Composable
private fun IptvContentCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = onClick,
    modifier: Modifier = Modifier,
    backgroundImageRes: Int? = null,
    topContent: @Composable () -> Unit = {}
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 4.dp,
        animationSpec = spring(),
        label = "elevation"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) IptvCardBorderFocused else IptvCardBorder,
        label = "border_color"
    )
    val animatedCardBg by animateColorAsState(
        targetValue = if (isSelected) IptvCardFocused else IptvCardBackground,
        label = "card_bg"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(225.dp)
            .height(345.dp)
            .shadow(animatedElevation, RoundedCornerShape(18.dp), spotColor = Color(0x66000000))
            .focusable(),
        shape = RoundedCornerShape(18.dp),
        color = animatedCardBg,
        border = BorderStroke(1.2.dp, animatedBorderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundImageRes != null) {
                Image(
                    painter = painterResource(backgroundImageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp)),
                    alpha = 0.22f
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1FFFFFFF),
                                    Color.Transparent,
                                    Color(0x22FFFFFF)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    topContent()
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = IptvTextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = IptvTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // THE FOCUSED UNDERLINE: Required white underline beneath text
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(3.5.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color.White, Color.Transparent),
                                            radius = 40f
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(IptvUnderlineGlow, shape = RoundedCornerShape(2.dp))
                                    .testTag("focused_underline_indicator")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom-Right Corner Widget:
 * - Current Time (e.g., "12:51")
 * - Date and Weather info ("Wednesday, Jan 2", "24°")
 */
@Composable
private fun BottomInfoWidget(
    timeString: String,
    dateString: String,
    temperature: String,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_widget_area"),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPreviewClick() }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_iptv_weather),
                    contentDescription = "Weather Icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = temperature,
                    color = IptvTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = timeString,
                color = IptvTextPrimary,
                fontSize = 46.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
                modifier = Modifier.testTag("current_time_widget")
            )

            Text(
                text = dateString,
                color = IptvTextSecondary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.testTag("current_date_widget")
            )
        }
    }
}

/**
 * Dynamic Content Drawer / Catalog (Live TV Channels, VOD Movies, Radio Stations)
 * Renders custom logos dynamically using Coil AsyncImage!
 */
@Composable
private fun DynamicMediaCatalogDrawer(
    mediaType: MediaType,
    channels: List<ChannelItem>,
    movies: List<MovieItem>,
    radios: List<RadioStation>,
    onSelectChannel: (ChannelItem) -> Unit,
    onSelectMovie: (MovieItem) -> Unit,
    onSelectRadio: (RadioStation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF2161A22),
        border = BorderStroke(1.dp, Color(0xFF2E384D)),
        shadowElevation = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_catalog_drawer")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (mediaType) {
                                    MediaType.LIVE_TV -> Color(0xFFE50914)
                                    MediaType.MOVIE -> Color(0xFF00897B)
                                    MediaType.RADIO -> Color(0xFF7B1FA2)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (mediaType) {
                                MediaType.LIVE_TV -> "LIVE TV FEED"
                                MediaType.MOVIE -> "4K VOD CINEMA"
                                MediaType.RADIO -> "GLOBAL RADIOS"
                            },
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = when (mediaType) {
                            MediaType.LIVE_TV -> "Channels (${channels.size})"
                            MediaType.MOVIE -> "Movies Collection (${movies.size})"
                            MediaType.RADIO -> "Radio Stations (${radios.size})"
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Close ✕",
                    color = IptvTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Horizontal Catalog Items with Custom Dynamic Logos
            when (mediaType) {
                MediaType.LIVE_TV -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(channels) { ch ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1F2633),
                                border = BorderStroke(1.dp, Color(0xFF2E384D)),
                                onClick = { onSelectChannel(ch) },
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("catalog_channel_${ch.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom Channel Logo dynamically rendered via Coil AsyncImage
                                    AsyncImage(
                                        model = ch.logoUrl,
                                        contentDescription = ch.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(8.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = ch.currentProgram,
                                            color = IptvTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap to Stream ▶",
                                            color = Color(0xFF4FC3F7),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MediaType.MOVIE -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(movies) { mov ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1F2633),
                                border = BorderStroke(1.dp, Color(0xFF2E384D)),
                                onClick = { onSelectMovie(mov) },
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("catalog_movie_${mov.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom Movie Poster dynamically rendered via Coil AsyncImage
                                    AsyncImage(
                                        model = mov.posterUrl,
                                        contentDescription = mov.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(58.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mov.title,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${mov.genre} • ★ ${mov.rating}",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Watch 4K ▶",
                                            color = Color(0xFF80CBC4),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MediaType.RADIO -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(radios) { rad ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1F2633),
                                border = BorderStroke(1.dp, Color(0xFF2E384D)),
                                onClick = { onSelectRadio(rad) },
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("catalog_radio_${rad.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom Radio Station Logo dynamically rendered via Coil AsyncImage
                                    AsyncImage(
                                        model = rad.logoUrl,
                                        contentDescription = rad.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .border(BorderStroke(1.dp, Color(0xFFCE93D8)), CircleShape)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rad.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${rad.frequency} • ${rad.genre}",
                                            color = IptvTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Listen Live ▶",
                                            color = Color(0xFFCE93D8),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
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
 * Interactive Channel & Player preview overlay matching the rich ecosystem in Screenshot 1
 */
@Composable
private fun ChannelQuickPreviewOverlay(
    channels: List<ChannelItem>,
    onSelectChannel: (ChannelItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF0181C24),
        border = BorderStroke(1.dp, Color(0xFF384357)),
        shadowElevation = 24.dp,
        modifier = modifier
            .width(580.dp)
            .testTag("channel_preview_overlay")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Featured Live Channels",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Close ✕",
                    color = IptvTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Channel Rows with Dynamic Custom Logos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val firstTwo = channels.take(2)
                firstTwo.forEach { ch ->
                    ChannelCardPreviewItem(
                        channel = ch,
                        onClick = { onSelectChannel(ch) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelCardPreviewItem(
    channel: ChannelItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222834))
            .border(BorderStroke(1.dp, Color(0xFF323B4C)), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Custom Logo dynamically rendered via Coil AsyncImage
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) Color(0xFFFF9800) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = channel.currentProgram,
                color = IptvTextSecondary,
                fontSize = 11.5.sp,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF333D4F))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "1080p FHD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF333D4F))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "EPG", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Tap to Stream ▶",
                    color = Color(0xFF4FC3F7),
                    fontSize = 11.sp
                )
            }
        }
    }
}
