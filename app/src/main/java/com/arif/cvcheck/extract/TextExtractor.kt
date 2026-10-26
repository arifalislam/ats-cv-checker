package com.arif.cvcheck.extract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.arif.cvcheck.ats.CvInput
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CvReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

enum class FileKind { PDF, DOCX, IMAGE }

data class ExtractedCv(
    val fileName: String,
    val kind: FileKind,
    val input: CvInput
)

/** Reads text out of PDF, DOCX and image CVs. Everything happens on the device. */
class TextExtractor(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extract(uri: Uri, onStage: (String) -> Unit = {}): ExtractedCv =
        withContext(Dispatchers.IO) {
            val name = displayName(uri)
            val mime = resolver.getType(uri).orEmpty().lowercase()
            val lowerName = name.lowercase()

            val kind = when {
                mime == "application/pdf" || lowerName.endsWith(".pdf") -> FileKind.PDF
                mime.contains("wordprocessingml") || lowerName.endsWith(".docx") -> FileKind.DOCX
                mime.startsWith("image/") || lowerName.matches(Regex(".*\\.(png|jpe?g|webp|bmp|heic)$")) -> FileKind.IMAGE
                lowerName.endsWith(".doc") -> throw CvReadException(
                    "Old .doc files are not supported. Save the CV as .docx or PDF and try again."
                )
                else -> throw CvReadException("Unsupported file type. Choose a PDF, DOCX, or an image (JPG/PNG).")
            }

            try {
                val input = when (kind) {
                    FileKind.PDF -> readPdf(uri, onStage)
                    FileKind.DOCX -> { onStage("Reading document…"); readDocx(uri) }
                    FileKind.IMAGE -> { onStage("Running text recognition…"); readImage(uri) }
                }
                ExtractedCv(name, kind, input)
            } catch (e: CvReadException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                throw CvReadException("This file is too large to process on this device.", e)
            } catch (e: Exception) {
                throw CvReadException("Could not read this file. It may be corrupted or password-protected.", e)
            }
        }

    // ---------------- PDF ----------------

    private suspend fun readPdf(uri: Uri, onStage: (String) -> Unit): CvInput {
        onStage("Reading PDF…")
        PDFBoxResourceLoader.init(appContext)

        var pages = 0
        var text = ""
        resolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
                if (doc.isEncrypted) throw CvReadException("This PDF is password-protected. Remove the password and try again.")
                pages = doc.numberOfPages
                text = PDFTextStripper().apply { sortByPosition = true }.getText(doc)
            }
        } ?: throw IOException("Cannot open file")

        // Little or no text means a scanned/image PDF: fall back to OCR on the first pages.
        if (text.trim().length < 80) {
            onStage("Scanned PDF detected — running text recognition…")
            val ocrText = ocrPdfPages(uri, maxPages = 3)
            return CvInput(text = ocrText, pageCount = pages.takeIf { it > 0 }, fromImageOrScan = true)
        }
        return CvInput(text = text, pageCount = pages.takeIf { it > 0 })
    }

    private suspend fun ocrPdfPages(uri: Uri, maxPages: Int): String {
        val pfd = resolver.openFileDescriptor(uri, "r") ?: throw IOException("Cannot open file")
        val sb = StringBuilder()
        pfd.use {
            PdfRenderer(it).use { renderer ->
                for (i in 0 until minOf(renderer.pageCount, maxPages)) {
                    renderer.openPage(i).use { page ->
                        val scale = 1800f / page.width
                        val bmp = Bitmap.createBitmap(
                            1800, (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888
                        )
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        sb.append(recognize(bmp)).append('\n')
                        bmp.recycle()
                    }
                }
            }
        }
        return sb.toString()
    }

    // ---------------- DOCX ----------------

    private fun readDocx(uri: Uri): CvInput {
        var documentXml: String? = null
        var pages: Int? = null

        resolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "word/document.xml" -> documentXml = zip.readBytesLimited().toString(Charsets.UTF_8)
                        "docProps/app.xml" -> {
                            val app = zip.readBytesLimited().toString(Charsets.UTF_8)
                            pages = Regex("<Pages>(\\d+)</Pages>").find(app)?.groupValues?.get(1)?.toIntOrNull()
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("Cannot open file")

        val xml = documentXml ?: throw CvReadException("This does not look like a valid .docx file.")

        val text = xml
            .replace(Regex("</w:p>"), "\n")
            .replace(Regex("<w:br\\s*/>"), "\n")
            .replace(Regex("<w:tab\\s*/>"), "\t")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'")
            .lines().joinToString("\n") { it.trimEnd() }

        return CvInput(
            text = text,
            pageCount = pages?.takeIf { it > 0 },
            hasTables = xml.contains("<w:tbl>") || xml.contains("<w:tbl "),
            hasTextBoxes = xml.contains("<w:txbxContent"),
            hasImages = xml.contains("<w:drawing") || xml.contains("<w:pict")
        )
    }

    private fun ZipInputStream.readBytesLimited(limit: Int = 25 * 1024 * 1024): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw CvReadException("This document is too large to process.")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    // ---------------- Image ----------------

    private suspend fun readImage(uri: Uri): CvInput {
        val bmp = decodeBitmap(uri) ?: throw CvReadException("Could not open this image.")
        val text = try { recognize(bmp) } finally { bmp.recycle() }
        return CvInput(text = text, pageCount = 1, fromImageOrScan = true)
    }

    private fun decodeBitmap(uri: Uri, maxSide: Int = 2400): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val rotation = try {
            resolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) { 0f }

        if (rotation == 0f) return decoded
        val m = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
            .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    }

    // ---------------- helpers ----------------

    private fun displayName(uri: Uri): String {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) c.getString(i)?.let { return it }
                }
            }
        }
        return uri.lastPathSegment ?: "CV"
    }
}
