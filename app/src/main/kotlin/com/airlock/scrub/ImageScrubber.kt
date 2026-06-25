package com.airlock.scrub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/** A redaction rectangle in normalized (0..1) image coordinates, so it is resolution-independent. */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class ExifTag(val label: String, val value: String, val sensitive: Boolean)

/**
 * Image privacy operations. Metadata removal works by **re-encoding the pixels** — this drops ALL
 * metadata (EXIF, XMP, IPTC, maker notes), not just the tags we know about. Redaction destroys the
 * underlying pixels; it is not a reversible overlay.
 */
object ImageScrubber {

    /** Human-readable list of the revealing metadata currently embedded in [uri]. */
    fun readExif(context: Context, uri: Uri): List<ExifTag> {
        val tags = mutableListOf<ExifTag>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)

                exif.latLong?.let { (lat, lon) ->
                    tags += ExifTag("GPS location", "%.6f, %.6f".format(lat, lon), sensitive = true)
                }
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                    tags += ExifTag("Date taken", it, sensitive = true)
                }
                exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
                    tags += ExifTag("Camera make", it, sensitive = false)
                }
                exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                    tags += ExifTag("Device model", it, sensitive = true)
                }
                exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let {
                    tags += ExifTag("Software", it, sensitive = false)
                }
                exif.getAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID)?.let {
                    tags += ExifTag("Unique image ID", it, sensitive = true)
                }
            }
        }
        return tags
    }

    /** Decode [uri], applying EXIF orientation so the pixels are upright before metadata is dropped. */
    fun loadOrientedBitmap(context: Context, uri: Uri, maxDim: Int? = null): Bitmap? = runCatching {
        val cr = context.contentResolver

        val sampleSize = if (maxDim == null) 1 else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var s = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / s > maxDim) s *= 2
            s
        }

        val bmp = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return@runCatching null

        val orientation = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        applyOrientation(bmp, orientation)
    }.getOrNull()

    /**
     * Produce a sanitized copy of [uri]: pixels re-encoded (all metadata gone), with [rects] painted
     * out irreversibly. Returns a shareable content:// uri, or null on failure.
     */
    fun scrub(context: Context, uri: Uri, rects: List<NormRect>): Uri? = runCatching {
        val oriented = loadOrientedBitmap(context, uri) ?: return@runCatching null
        val mutable = if (oriented.isMutable) oriented else oriented.copy(Bitmap.Config.ARGB_8888, true)

        if (rects.isNotEmpty()) {
            val canvas = Canvas(mutable)
            val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            val w = mutable.width
            val h = mutable.height
            for (r in rects) {
                canvas.drawRect(r.left * w, r.top * h, r.right * w, r.bottom * h, paint)
            }
        }

        val isPng = (context.contentResolver.getType(uri) ?: "").contains("png")
        val ext = if (isPng) "png" else "jpg"
        val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        // Stable-ish unique name without Date APIs: nanoTime is fine for a cache file.
        val out = File(dir, "airlock_${System.nanoTime()}.$ext")
        FileOutputStream(out).use { mutable.compress(format, 95, it) }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
    }.getOrNull()

    /** Best-effort cleanup of previously exported files so the cache does not grow unbounded. */
    fun clearExports(context: Context) {
        runCatching { File(context.cacheDir, "shared").listFiles()?.forEach { it.delete() } }
    }

    private fun applyOrientation(bmp: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
