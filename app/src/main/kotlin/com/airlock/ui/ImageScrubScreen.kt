package com.airlock.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlock.scrub.ExifTag
import com.airlock.scrub.ImageScrubber
import com.airlock.scrub.NormRect
import com.airlock.intent.ShareIntents
import com.airlock.ui.theme.Accent
import com.airlock.ui.theme.Danger
import com.airlock.ui.theme.Good
import com.airlock.ui.theme.TextMuted
import com.airlock.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImageScrubScreen(
    uris: List<Uri>,
    stats: SessionStats,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var index by remember { mutableIntStateOf(0) }
    val current = uris[index.coerceIn(0, uris.lastIndex)]

    val redactions: SnapshotStateMap<Uri, List<NormRect>> = remember { mutableStateMapOf() }
    var display by remember { mutableStateOf<ImageBitmap?>(null) }
    var exif by remember { mutableStateOf<List<ExifTag>>(emptyList()) }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        display = null
        val loaded: Bitmap? = withContext(Dispatchers.IO) {
            ImageScrubber.loadOrientedBitmap(context, current, maxDim = 1600)
        }
        display = loaded?.asImageBitmap()
        exif = withContext(Dispatchers.IO) { ImageScrubber.readExif(context, current) }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AppHeader(if (uris.size > 1) "Clean images (${index + 1}/${uris.size})" else "Clean image", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            val img = display
            if (img == null) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                RedactionImage(
                    image = img,
                    rects = redactions[current] ?: emptyList(),
                    onAddRect = { r -> redactions[current] = (redactions[current] ?: emptyList()) + r },
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cur = redactions[current] ?: emptyList()
                            if (cur.isNotEmpty()) redactions[current] = cur.dropLast(1)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Undo, null, modifier = Modifier.height(18.dp))
                        Text(" Undo box")
                    }
                    if (uris.size > 1) {
                        OutlinedButton(onClick = { if (index > 0) index-- }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                        }
                        OutlinedButton(onClick = { if (index < uris.lastIndex) index++ }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                        }
                    }
                }
                Text(
                    "Drag on the image to paint an irreversible black box over anything sensitive.",
                    color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            ExifReport(exif)

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (exporting) return@Button
                    exporting = true
                    scope.launch {
                        val out = withContext(Dispatchers.IO) {
                            uris.mapNotNull { u -> ImageScrubber.scrub(context, u, redactions[u] ?: emptyList()) }
                        }
                        stats.imagesCleaned += out.size
                        exporting = false
                        if (out.isNotEmpty()) ShareIntents.shareImages(context, out)
                    }
                },
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (exporting) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.height(20.dp))
                } else {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.height(18.dp))
                    Text(if (uris.size > 1) "  Export all clean (${uris.size})" else "  Export clean", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RedactionImage(
    image: ImageBitmap,
    rects: List<NormRect>,
    onAddRect: (NormRect) -> Unit,
) {
    var active by remember { mutableStateOf<NormRect?>(null) }
    var startX by remember { mutableStateOf(0f) }
    var startY by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .pointerInput(image) {
                detectDragGestures(
                    onDragStart = { o ->
                        startX = (o.x / size.width).coerceIn(0f, 1f)
                        startY = (o.y / size.height).coerceIn(0f, 1f)
                        active = NormRect(startX, startY, startX, startY)
                    },
                    onDrag = { change, _ ->
                        val cx = (change.position.x / size.width).coerceIn(0f, 1f)
                        val cy = (change.position.y / size.height).coerceIn(0f, 1f)
                        active = NormRect(min(startX, cx), min(startY, cy), max(startX, cx), max(startY, cy))
                    },
                    onDragEnd = {
                        active?.let { if (it.right - it.left > 0.01f && it.bottom - it.top > 0.01f) onAddRect(it) }
                        active = null
                    },
                )
            },
    ) {
        androidx.compose.foundation.Image(
            bitmap = image,
            contentDescription = "Image being cleaned",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Canvas(Modifier.matchParentSize()) {
            (rects + listOfNotNull(active)).forEach { r ->
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(r.left * size.width, r.top * size.height),
                    size = Size((r.right - r.left) * size.width, (r.bottom - r.top) * size.height),
                )
            }
        }
    }
}

@Composable
private fun ExifReport(exif: List<ExifTag>) {
    Card {
        Text("Metadata in this image", color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (exif.isEmpty()) {
            Text("No EXIF metadata found. Exporting still re-encodes the pixels to be safe.", color = Good, fontSize = 14.sp)
        } else {
            exif.forEach { tag ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        tag.label,
                        color = if (tag.sensitive) Danger else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.45f),
                    )
                    Text(
                        tag.value,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.55f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("All of this is destroyed on export.", color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
