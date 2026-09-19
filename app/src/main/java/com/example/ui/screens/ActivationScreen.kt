package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ActivationResult
import com.example.data.CodeType
import com.example.data.IptvRepository
import com.example.ui.theme.IptvAccentOrange
import com.example.ui.theme.IptvBackground
import com.example.ui.theme.IptvBackgroundDark
import com.example.ui.theme.IptvCardBackground
import com.example.ui.theme.IptvCardBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.theme.IptvTextMuted
import com.example.ui.theme.IptvTextSecondary
import kotlinx.coroutines.launch

/**
 * Login / Activation Screen
 * - Dedicated activation interface where users verify their subscription code against remote storage
 * - Checks Private individual code vs Universal subscription code and expiration timestamps
 * - Clear WhatsApp contact card & error alerts displaying support number: +212643316085
 */
@Composable
fun ActivationScreen(
    onActivationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adminConfig by IptvRepository.adminConfig.collectAsState()

    var activationCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDialogInfo by remember { mutableStateOf<ActivationResult?>(null) }
    val whatsappNumber = "+212643316085"

    fun openWhatsAppChat(customMessage: String = "Hello, I would like to buy or renew an EURO IPTV activation code") {
        try {
            val cleanPhone = whatsappNumber.replace("+", "").replace(" ", "")
            val encodedMsg = Uri.encode(customMessage)
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Contact WhatsApp: $whatsappNumber", Toast.LENGTH_LONG).show()
        }
    }

    fun performActivation() {
        if (activationCode.isBlank()) {
            errorMessage = "Please enter your activation code"
            return
        }

        errorMessage = null
        isVerifying = true

        coroutineScope.launch {
            val result = IptvRepository.verifyActivationCodeRemote(activationCode)
            isVerifying = false

            when (result) {
                is ActivationResult.Success -> {
                    val rec = result.record
                    val typeLabel = if (rec.type == CodeType.PRIVATE) "Private License" else "Universal VIP"
                    try {
                        Toast.makeText(
                            context,
                            "Activated successfully: $typeLabel (${rec.planName})",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {
                        // Safe fallback in test or headless environments
                    }
                    onActivationSuccess()
                }
                is ActivationResult.Expired -> {
                    errorMessage = result.message
                    errorDialogInfo = result
                }
                is ActivationResult.Invalid -> {
                    errorMessage = result.message
                    errorDialogInfo = result
                }
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
            .testTag("activation_screen"),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val isWide = maxWidth >= 700.dp
            val cardWidth = if (isWide) 520.dp else maxWidth * 0.9f

            Surface(
                modifier = Modifier
                    .width(cardWidth)
                    .widthIn(max = 560.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                color = IptvCardBackground,
                border = BorderStroke(1.2.dp, IptvCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top App Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2838))
                                .border(BorderStroke(1.dp, Color(0xFF384355)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Activation Key",
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "EURO IPTV PRO",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Cloud Remote Authentication Service",
                                color = IptvTextSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Enter your subscription activation code to unlock Live TV streams, VOD Cinema, and Global Radios.",
                        color = IptvTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // Activation Code Input Field
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = {
                            activationCode = it
                            errorMessage = null
                        },
                        label = { Text("Subscription Activation Code") },
                        placeholder = { Text("e.g. EURO2026, VIP-IPTV-888, PRIV-...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = IptvAccentOrange
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4FC3F7),
                            unfocusedBorderColor = Color(0xFF333E50),
                            focusedLabelColor = Color(0xFF4FC3F7),
                            unfocusedLabelColor = IptvTextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF4FC3F7)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_code_input")
                    )

                    // Inline Error Notice (if any)
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33E53935))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFFF6E6E),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // "Activate Now" Primary Submit Button
                    Button(
                        onClick = { performActivation() },
                        enabled = !isVerifying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2979FF),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E3A6E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("activate_submit_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verifying with Database...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Activate Now",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick Demo Codes helper row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test codes:",
                            color = IptvTextMuted,
                            fontSize = 11.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("EURO2026", "VIP-IPTV-888", "PRIV-AHMED-2026").forEach { demo ->
                                Text(
                                    text = demo,
                                    color = Color(0xFF4FC3F7),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            activationCode = demo
                                            errorMessage = null
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ========================================================
                    // WhatsApp Support Card (Requirement 1)
                    // ========================================================
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E2532),
                        border = BorderStroke(1.dp, Color(0xFF2E384D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_support_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = "Support",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.whatsapp_support_label),
                                    color = IptvTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Interactive WhatsApp Button
                            Button(
                                onClick = { openWhatsAppChat() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF192A23),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("whatsapp_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = whatsappNumber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• Chat",
                                        fontSize = 12.sp,
                                        color = Color(0xFF25D366),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // EXPIRED / INVALID CODE ERROR ALERT DIALOG
        // ========================================================
        if (errorDialogInfo != null) {
            val isExpired = errorDialogInfo is ActivationResult.Expired
            val title = if (isExpired) "Subscription Expired" else "Invalid Activation Code"
            val message = when (val res = errorDialogInfo) {
                is ActivationResult.Expired -> "${res.message}\n\nPlease contact our WhatsApp support team to renew your subscription immediately."
                is ActivationResult.Invalid -> "${res.message}\n\nNeed an activation code? Contact our official WhatsApp support representative."
                else -> "Please verify your code."
            }

            AlertDialog(
                onDismissRequest = { errorDialogInfo = null },
                containerColor = Color(0xFF1E2430),
                titleContentColor = Color.White,
                textContentColor = IptvTextSecondary,
                icon = {
                    Icon(
                        imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.ErrorOutline,
                        contentDescription = "Alert",
                        tint = if (isExpired) Color(0xFFFF9800) else Color(0xFFE53935),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = message,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFFD8DFEB)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF263238))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "WhatsApp Support: $whatsappNumber",
                                color = Color(0xFF80CBC4),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val msg = if (isExpired) {
                                "Hello, my EURO IPTV code ($activationCode) has expired. I would like to renew."
                            } else {
                                "Hello, I need an official EURO IPTV subscription activation code."
                            }
                            openWhatsAppChat(msg)
                            errorDialogInfo = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contact WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { errorDialogInfo = null }) {
                        Text("Try Another Code", color = IptvTextSecondary)
                    }
                }
            )
        }
    }
}
