package com.airlock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airlock.scrub.ImageScrubber
import com.airlock.scrub.NormRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ImageScrubberInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun makeJpegWithExif(): Uri {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.RED)
        val file = File(context.cacheDir, "exif_src.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        val exif = ExifInterface(file.absolutePath)
        exif.setLatLong(48.858844, 2.294351) // Eiffel Tower
        exif.setAttribute(ExifInterface.TAG_MAKE, "ACME")
        exif.setAttribute(ExifInterface.TAG_MODEL, "SecretPhone X")
        exif.saveAttributes()

        // sanity: the source really has GPS
        val check = ExifInterface(file.absolutePath)
        assertNotNull("source should contain GPS", check.latLong)
        return Uri.fromFile(file)
    }

    @Test
    fun scrubRemovesAllExif() {
        val src = makeJpegWithExif()
        val out = ImageScrubber.scrub(context, src, emptyList())
        assertNotNull("scrub should produce an output uri", out)

        context.contentResolver.openInputStream(out!!).use { input ->
            val exif = ExifInterface(input!!)
            assertNull("GPS must be gone after scrub", exif.latLong)
            assertNull("device model must be gone after scrub", exif.getAttribute(ExifInterface.TAG_MODEL))
            assertNull("camera make must be gone after scrub", exif.getAttribute(ExifInterface.TAG_MAKE))
        }
    }

    @Test
    fun redactionDestroysPixels() {
        val src = makeJpegWithExif() // red image
        // Redact the whole image.
        val out = ImageScrubber.scrub(context, src, listOf(NormRect(0f, 0f, 1f, 1f)))
        assertNotNull(out)

        val bmp = context.contentResolver.openInputStream(out!!).use {
            BitmapFactory.decodeStream(it)
        }
        assertNotNull(bmp)
        val center = bmp!!.getPixel(bmp.width / 2, bmp.height / 2)
        // JPEG is lossy; assert the pixel is near-black, not the original red.
        assertTrue("redacted pixel should be dark", Color.red(center) < 40 && Color.green(center) < 40 && Color.blue(center) < 40)
    }

    @Test
    fun outputIsShareableContentUri() {
        val src = makeJpegWithExif()
        val out = ImageScrubber.scrub(context, src, emptyList())
        assertEquals("content", out!!.scheme)
    }
}
