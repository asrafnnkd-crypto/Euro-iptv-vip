package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.ChannelCategory
import com.example.data.ChannelItem
import com.example.data.IptvRepository
import com.example.ui.theme.IptvAccentOrange
import com.example.ui.theme.IptvBackground
import com.example.ui.theme.IptvBackgroundDark
import com.example.ui.theme.IptvCardBackground
import com.example.ui.theme.IptvCardBorder
import com.example.ui.theme.IptvTextMuted
import com.example.ui.theme.IptvTextSecondary
import java.util.UUID

/**
 * Admin Panel Screen (Remote Dashboard Integration Readiness)
 * - Complete architecture to be controlled via remote Admin Panel (Dashboard)
 * - Dynamic remote features:
 *   1. Adding / Deleting stream channels dynamically
 *   2. Managing categories dynamically
 *   3. Updating app remote configurations (Server API URL, WhatsApp number, MOTD announcement, Demo activation codes)
 */
@Composable
fun AdminPanelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminConfig by IptvRepository.adminConfig.collectAsState()
    val channels by IptvRepository.channels.collectAsState()
    val categories by IptvRepository.categories.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Channels, 1 = Categories, 2 = Remote Config

    // Channel Form State
    var newChannelName by remember { mutableStateOf("") }
    var newChannelUrl by remember { mutableStateOf("") }
    var newChannelLogoUrl by remember { mutableStateOf("") }
    var newChannelCategory by remember { mutableStateOf("cat_sports") }
    var isSyncingRemote by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Category Form State
    var newCategoryName by remember { mutableStateOf("") }

    // Config Form State
    var configServerUrl by remember(adminConfig) { mutableStateOf(adminConfig.serverUrl) }
    var configJsonBinId by remember(adminConfig) { mutableStateOf(adminConfig.jsonBinId) }
    var configJsonBinApiKey by remember(adminConfig) { mutableStateOf(adminConfig.jsonBinApiKey) }
    var configSupportWhatsapp by remember(adminConfig) { mutableStateOf(adminConfig.supportWhatsapp) }
    var configMotd by remember(adminConfig) { mutableStateOf(adminConfig.motd) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF222731), IptvBackground, IptvBackgroundDark),
                    radius = 1600f
                )
            )
            .testTag("admin_panel_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF272D39))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Admin Panel & Remote Controller",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Remote IPTV Engine Synchronizer • Connected to Cloud API",
                            color = IptvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Cloud Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E3224))
                        .border(BorderStroke(1.dp, Color(0xFF2F663C)), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Admin Synced", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Navigation: Channels / Categories / Remote Config
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1F242F),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, IptvCardBorder), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stream Channels (${channels.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Categories (${categories.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Remote Server Config", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Content based on Tab
            when (selectedTab) {
                0 -> {
                    // CHANNELS MANAGEMENT
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left: Add Channel Form
                        Surface(
                            modifier = Modifier.width(360.dp).fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            color = IptvCardBackground,
                            border = BorderStroke(1.dp, IptvCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Add Remote Channel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = newChannelName,
                                    onValueChange = { newChannelName = it },
                                    label = { Text("Channel Name") },
                                    placeholder = { Text("e.g. EURO Sports 4K") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = newChannelUrl,
                                    onValueChange = { newChannelUrl = it },
                                    label = { Text("HLS / M3U8 Stream URL") },
                                    placeholder = { Text("https://stream.server/live.m3u8") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = newChannelLogoUrl,
                                    onValueChange = { newChannelLogoUrl = it },
                                    label = { Text("Custom Logo Image URL") },
                                    placeholder = { Text("https://example.com/logo.png") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (newChannelName.isNotBlank() && newChannelUrl.isNotBlank()) {
                                            IptvRepository.addChannel(
                                                ChannelItem(
                                                    id = "ch_" + UUID.randomUUID().toString().take(8),
                                                    name = newChannelName.trim(),
                                                    categoryId = newChannelCategory,
                                                    streamUrl = newChannelUrl.trim(),
                                                    logoUrl = newChannelLogoUrl.trim().ifBlank { null },
                                                    currentProgram = "Admin Configured Stream"
                                                )
                                            )
                                            newChannelName = ""
                                            newChannelUrl = ""
                                            newChannelLogoUrl = ""
                                            Toast.makeText(context, "Channel added to remote feed!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please fill in channel name and URL", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Push Channel to App", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Right: Channels List
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            color = IptvCardBackground,
                            border = BorderStroke(1.dp, IptvCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Live Dynamic Channels", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(channels) { ch ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1B202A))
                                                .border(BorderStroke(1.dp, Color(0xFF2C3545)), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (!ch.logoUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = ch.logoUrl,
                                                        contentDescription = ch.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF2B3342)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                Column {
                                                    Text(ch.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(ch.streamUrl, color = IptvTextSecondary, fontSize = 11.sp, maxLines = 1)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    IptvRepository.deleteChannel(ch.id)
                                                    Toast.makeText(context, "Channel removed", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // CATEGORIES MANAGEMENT
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.width(360.dp).fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            color = IptvCardBackground,
                            border = BorderStroke(1.dp, IptvCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Create New Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Category Name") },
                                    placeholder = { Text("e.g. 4K Ultra Cinema") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (newCategoryName.isNotBlank()) {
                                            IptvRepository.addCategory(
                                                ChannelCategory(
                                                    id = "cat_" + UUID.randomUUID().toString().take(6),
                                                    name = newCategoryName.trim()
                                                )
                                            )
                                            newCategoryName = ""
                                            Toast.makeText(context, "Category created successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Category", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            color = IptvCardBackground,
                            border = BorderStroke(1.dp, IptvCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Configured Categories", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1B202A))
                                                .border(BorderStroke(1.dp, Color(0xFF2C3545)), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cat.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                            IconButton(
                                                onClick = {
                                                    IptvRepository.deleteCategory(cat.id)
                                                    Toast.makeText(context, "Category removed", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // REMOTE SERVER & APP CONFIGURATION
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        color = IptvCardBackground,
                        border = BorderStroke(1.dp, IptvCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Altero Remote Cloud & Data Source (jsonbin.io)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Real-time synchronization for live TV, movies, radios, and multi-tier activation codes.", color = IptvTextSecondary, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(20.dp))

                            // JSONBin.io Dedicated Integration Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B202A),
                                border = BorderStroke(1.dp, Color(0xFF323D52))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Target Platform: jsonbin.io", color = Color(0xFF2979FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (adminConfig.lastSyncStatus.startsWith("Success")) Color(0xFF1B5E20) else Color(0xFFE65100)
                                        ) {
                                            Text(
                                                text = if (adminConfig.lastSyncStatus.startsWith("Success")) "ONLINE / SYNCED" else "READY / FALLBACK ACTIVE",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Endpoint: https://api.jsonbin.io/v3/b/$configJsonBinId", color = IptvTextSecondary, fontSize = 12.sp)
                                    Text("Sync Status: ${adminConfig.lastSyncStatus}", color = Color.White, fontSize = 12.sp)
                                    Text("Last Check: ${adminConfig.lastSyncTime}", color = IptvTextMuted, fontSize = 11.sp)

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = configJsonBinId,
                                            onValueChange = { configJsonBinId = it },
                                            label = { Text("jsonbin.io Bin ID") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        OutlinedTextField(
                                            value = configJsonBinApiKey,
                                            onValueChange = { configJsonBinApiKey = it },
                                            label = { Text("API Key (X-Master-Key / X-Access-Key)") },
                                            placeholder = { Text("Optional if Bin is public") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.4f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                isSyncingRemote = true
                                                coroutineScope.launch {
                                                    val res = IptvRepository.fetchFromJsonBin(configJsonBinId.trim(), configJsonBinApiKey.trim())
                                                    isSyncingRemote = false
                                                    when (res) {
                                                        is com.example.data.JsonBinFetchResult.Success -> {
                                                            Toast.makeText(context, "Success! Fetched ${res.codesCount} codes, ${res.channelsCount} channels, ${res.moviesCount} movies, ${res.radiosCount} radios.", Toast.LENGTH_LONG).show()
                                                        }
                                                        is com.example.data.JsonBinFetchResult.AuthRequired -> {
                                                            Toast.makeText(context, "Bin is private! Please supply X-Master-Key or set bin public.", Toast.LENGTH_LONG).show()
                                                        }
                                                        is com.example.data.JsonBinFetchResult.Error -> {
                                                            Toast.makeText(context, "Fetch notice: ${res.message}. Fallback catalog active.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isSyncingRemote,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF), contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isSyncingRemote) "Syncing with jsonbin.io..." else "Fetch from jsonbin.io", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Secondary Server & Broadcast Announcements", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = configServerUrl,
                                onValueChange = { configServerUrl = it },
                                label = { Text("Primary Cloud Server / M3U API Endpoint") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = configSupportWhatsapp,
                                onValueChange = { configSupportWhatsapp = it },
                                label = { Text("Support & Subscription Sales WhatsApp Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = configMotd,
                                onValueChange = { configMotd = it },
                                label = { Text("Remote Broadcast Announcement (MOTD)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = {
                                        IptvRepository.updateAdminConfig(
                                            adminConfig.copy(
                                                serverUrl = configServerUrl.trim(),
                                                jsonBinId = configJsonBinId.trim(),
                                                jsonBinApiKey = configJsonBinApiKey.trim(),
                                                supportWhatsapp = configSupportWhatsapp.trim(),
                                                motd = configMotd.trim()
                                            )
                                        )
                                        Toast.makeText(context, "Remote configuration deployed successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save & Deploy Config", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
