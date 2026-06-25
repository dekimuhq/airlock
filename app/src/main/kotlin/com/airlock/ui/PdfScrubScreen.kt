package com.airlock.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlock.intent.ShareIntents
import com.airlock.scrub.PdfScrubber
import com.airlock.ui.theme.Accent
import com.airlock.ui.theme.Danger
import com.airlock.ui.theme.Good
import com.airlock.ui.theme.TextMuted
import com.airlock.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfScrubScreen(
    uri: Uri,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var meta by remember { mutableStateOf<List<PdfScrubber.PdfMeta>?>(null) }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        meta = withContext(Dispatchers.IO) { PdfScrubber.readMetadata(context, uri) }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AppHeader("Clean PDF", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PictureAsPdf, null, tint = Accent, modifier = Modifier.height(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("  Document selected", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            val m = meta
            when {
                m == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                else -> MetaReport(m)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (exporting) return@Button
                    exporting = true
                    scope.launch {
                        val out = withContext(Dispatchers.IO) { PdfScrubber.scrub(context, uri) }
                        exporting = false
                        if (out != null) ShareIntents.sharePdf(context, out)
                    }
                },
                enabled = !exporting && meta != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (exporting) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.height(20.dp))
                } else {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.height(18.dp))
                    Text("  Export clean PDF", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MetaReport(meta: List<PdfScrubber.PdfMeta>) {
    Card {
        Text("Metadata in this PDF", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (meta.isEmpty()) {
            Text("No document metadata found. Exporting still rewrites the file to be safe.", color = Good, fontSize = 14.sp)
        } else {
            meta.forEach { tag ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        tag.label,
                        color = if (tag.sensitive) Danger else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.4f),
                    )
                    Text(
                        tag.value,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.6f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("The Info dictionary and XMP stream are removed on export.", color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
