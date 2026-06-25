package com.airlock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlock.core.LinkScrubber
import com.airlock.core.TextCleanResult
import com.airlock.data.ScrubSettings
import com.airlock.intent.ShareIntents
import com.airlock.ui.theme.Accent
import com.airlock.ui.theme.Danger
import com.airlock.ui.theme.Good
import com.airlock.ui.theme.TextMuted
import com.airlock.ui.theme.TextPrimary

@Composable
fun TextScrubScreen(
    initialText: String,
    settings: ScrubSettings,
    stats: SessionStats,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var input by remember { mutableStateOf(initialText) }
    var result by remember { mutableStateOf<TextCleanResult?>(null) }

    fun clean() {
        val scrubber = LinkScrubber(extraExactParams = settings.customParams)
        val r = scrubber.cleanText(
            text = input,
            redactPii = settings.redactPiiInText,
            piiTypes = settings.enabledPiiTypes,
        )
        result = r
        if (r.changed || r.urlsProcessed > 0) {
            stats.linksCleaned += r.urlsProcessed
            stats.paramsRemoved += r.removedParams.size
            stats.piiRedacted += r.redactedPii.size
        }
    }

    // Auto-clean when arriving from a Share/Process-text intent.
    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) clean()
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AppHeader("Clean link or text", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Paste a link or text") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { clean() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clean", fontWeight = FontWeight.Bold)
            }

            val r = result
            if (r != null) {
                Spacer(Modifier.height(20.dp))
                ResultReport(r)

                Spacer(Modifier.height(16.dp))
                Card {
                    Text("Clean output", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            r.cleaned.ifBlank { "(empty)" },
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(r.cleaned)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.height(0.dp)); Text(" Copy")
                    }
                    Button(
                        onClick = { ShareIntents.shareText(context, r.cleaned) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.height(18.dp))
                        Text(" Share")
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ResultReport(r: TextCleanResult) {
    Card {
        Text("Privacy report", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (!r.changed) {
            Text("Already clean — nothing to remove.", color = Good, fontSize = 14.sp)
            return@Card
        }
        ReportLine("Links processed", "${r.urlsProcessed}")
        if (r.unwrappedCount > 0) ReportLine("Redirects unwrapped", "${r.unwrappedCount}", Accent)
        if (r.removedParams.isNotEmpty()) {
            ReportLine("Trackers removed", "${r.removedParams.size}", Danger)
            Spacer(Modifier.height(6.dp))
            Text(
                r.removedParams.joinToString("  ·  ") { it.substringBefore('=') },
                color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            )
        }
        if (r.redactedPii.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            ReportLine("PII redacted", "${r.redactedPii.size}", Danger)
            Spacer(Modifier.height(4.dp))
            Text(
                r.redactedPii.joinToString("  ·  ") { it.type.label },
                color = TextMuted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
