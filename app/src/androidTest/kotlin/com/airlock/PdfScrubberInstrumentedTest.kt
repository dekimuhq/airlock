package com.airlock

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airlock.scrub.PdfScrubber
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfScrubberInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun setup() = PDFBoxResourceLoader.init(context)

    private fun makePdfWithMetadata(): Uri {
        val file = File(context.cacheDir, "meta_src.pdf")
        PDDocument().use { doc ->
            doc.addPage(PDPage())
            doc.documentInformation.apply {
                author = "Jane Whistleblower"
                title = "Sensitive Report"
                producer = "SecretWriter 1.0"
            }
            doc.save(file)
        }
        return Uri.fromFile(file)
    }

    @Test fun readMetadataFindsAuthor() {
        val meta = PdfScrubber.readMetadata(context, makePdfWithMetadata())
        assertTrue("should report the author", meta.any { it.label == "Author" && it.value == "Jane Whistleblower" })
    }

    @Test fun scrubRemovesAllMetadata() {
        val out = PdfScrubber.scrub(context, makePdfWithMetadata())
        assertNotNull("scrub should produce an output uri", out)

        context.contentResolver.openInputStream(out!!).use { input ->
            PDDocument.load(input).use { doc ->
                val info = doc.documentInformation
                assertNull("author must be gone", info.author)
                assertNull("title must be gone", info.title)
                assertNull("producer must be gone", info.producer)
                assertNull("XMP metadata must be gone", doc.documentCatalog?.metadata)
            }
        }
    }

    @Test fun outputIsShareableContentUri() {
        val out = PdfScrubber.scrub(context, makePdfWithMetadata())
        assertNotNull(out)
        org.junit.Assert.assertEquals("content", out!!.scheme)
    }
}
