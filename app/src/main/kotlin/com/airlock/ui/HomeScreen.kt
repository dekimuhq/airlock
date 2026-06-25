package com.airlock.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlock.ui.theme.Accent
import com.airlock.ui.theme.OnAccent
import com.airlock.ui.theme.TextMuted
import com.airlock.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    stats: SessionStats,
    onCleanText: () -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onSettings: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris -> onImagesPicked(uris) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Airlock", fontWeight = FontWeight.Black, fontSize = 30.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextMuted)
            }
        }
        Text(
            "The airlock between your data and the world.",
            color = TextMuted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(16.dp))
        NoInternetBadge()

        Spacer(Modifier.height(24.dp))

        ActionTile(
            icon = Icons.Filled.Image,
            title = "Clean an image",
            subtitle = "Strip GPS & metadata · redact regions · re-encode",
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
        Spacer(Modifier.height(14.dp))
        ActionTile(
            icon = Icons.Filled.Link,
            title = "Clean a link or text",
            subtitle = "Remove trackers · unwrap redirects · redact PII",
            onClick = onCleanText,
        )

        Spacer(Modifier.height(28.dp))
        Text("This session", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("${stats.imagesCleaned}", "images", Modifier.weight(1f))
            StatPill("${stats.linksCleaned}", "links", Modifier.weight(1f))
            StatPill("${stats.paramsRemoved}", "trackers", Modifier.weight(1f))
            StatPill("${stats.piiRedacted}", "PII hidden", Modifier.weight(1f))
        }

        Spacer(Modifier.height(28.dp))
        Card {
            Text("Tip: share into Airlock", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "In any app, tap Share and choose Airlock. Your photo or link goes in, a sanitized copy comes out — without ever touching the network.",
                color = TextMuted, fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = OnAccent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(subtitle, color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
