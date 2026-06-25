package com.airlock.scrub

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Strips metadata from PDFs. A PDF hides identity in two places: the Info dictionary (Author,
 * Producer, Creator, timestamps) and an optional XMP metadata stream on the document catalog.
 * Airlock clears both, fully offline, via PDFBox-Android.
 */
object PdfScrubber {

    data class PdfMeta(val label: String, val value: String, val sensitive: Boolean)

    fun readMetadata(context: Context, uri: Uri): List<PdfMeta> {
        val out = mutableListOf<PdfMeta>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { doc ->
                    val info = doc.documentInformation
                    info.author?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Author", it, true) }
                    info.creator?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Creator app", it, false) }
                    info.producer?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Producer", it, false) }
                    info.title?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Title", it, false) }
                    info.subject?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Subject", it, false) }
                    info.keywords?.takeIf { it.isNotBlank() }?.let { out += PdfMeta("Keywords", it, false) }
                    runCatching { info.creationDate }.getOrNull()?.let {
                        out += PdfMeta("Created", DATE_FMT.format(it.time), true)
                    }
                    runCatching { info.modificationDate }.getOrNull()?.let {
                        out += PdfMeta("Modified", DATE_FMT.format(it.time), true)
                    }
                    if (doc.documentCatalog?.metadata != null) {
                        out += PdfMeta("XMP metadata", "embedded", true)
                    }
                }
            }
        }
        return out
    }

    /** Produce a metadata-free copy of [uri]. Returns a shareable content:// uri, or null. */
    fun scrub(context: Context, uri: Uri): Uri? = runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val outFile = File(dir, "airlock_${System.nanoTime()}.pdf")

        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                doc.documentInformation = PDDocumentInformation() // empty Info dictionary
                doc.documentCatalog?.metadata = null               // drop the XMP stream
                FileOutputStream(outFile).use { doc.save(it) }
            }
        } ?: return@runCatching null

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }.getOrNull()

    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
}
