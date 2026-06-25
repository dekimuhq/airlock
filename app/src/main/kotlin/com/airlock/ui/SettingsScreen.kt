package com.airlock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.airlock.core.PiiType
import com.airlock.data.ScrubSettings
import com.airlock.data.SettingsRepository
import com.airlock.ui.theme.Accent
import com.airlock.ui.theme.Good
import com.airlock.ui.theme.TextMuted
import com.airlock.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: ScrubSettings,
    repo: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var newParam by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AppHeader("Settings", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Redact PII in text", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Hide emails, phones, cards, IPs and IBANs found in shared text.", color = TextMuted, fontSize = 13.sp)
                    }
                    Switch(
                        checked = settings.redactPiiInText,
                        onCheckedChange = { v -> scope.launch { repo.setRedactPii(v) } },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Card {
                Text("PII types to redact", color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                PiiType.entries.forEach { type ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(type.label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = type in settings.enabledPiiTypes,
                            onCheckedChange = { v -> scope.launch { repo.togglePiiType(type, v) } },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Card {
                Text("Custom tracking parameters", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Add your own param names to strip from links (e.g. affiliate, partner_id).", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newParam,
                        onValueChange = { newParam = it },
                        label = { Text("param name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = {
                        val p = newParam
                        newParam = ""
                        scope.launch { repo.addCustomParam(p) }
                    }) { Icon(Icons.Filled.Add, "Add", tint = Accent) }
                }
                if (settings.customParams.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    settings.customParams.sorted().forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { repo.removeCustomParam(p) } }) {
                                Icon(Icons.Filled.Close, "Remove", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Card {
                Text("Why you can trust Airlock", color = Good, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Airlock has no INTERNET permission. Check Android Settings → Apps → Airlock → Permissions: there is no network access to grant. It cannot upload your photos, links, or text anywhere. Everything happens on this device.",
                    color = TextMuted, fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text("Airlock · open source · Apache-2.0", color = TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
