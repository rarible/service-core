package com.rarible.core.meta.resource.detector

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.test.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path

class PngDetectorTest {
    @Test
    fun `the image is of apng type`() {
        val apngBytes = readFile("/meta/resource/detector/core/btc.png")
            ?: throw IllegalStateException("Could not find apng file")

        val contentBytes = ContentData.EMPTY.copy(data = apngBytes)
        val expectedApng = ContentMeta(mimeType = MimeType.APNG_IMAGE.value, width = 64, height = 64)
        assertEquals(expectedApng, PngDetector.detect(contentBytes, ""))
    }

    @Test
    fun `the image is of png type`() {
        val pngBytes = readFile("/meta/resource/detector/core/btc-icon.png")
            ?: throw IllegalStateException("Could not find apng file")
        val contentBytes = ContentData.EMPTY.copy(data = pngBytes)
        val expectedApng = ContentMeta(mimeType = MimeType.PNG_IMAGE.value, width = 64, height = 64)
        assertEquals(expectedApng, PngDetector.detect(contentBytes, ""))
    }

    @Test
    fun zaaptos() {
        val pngBytes = Files.readAllBytes(Path("""C:\Users\iosan\AppData\Local\Temp\meta.bin"""))
        val contentBytes = ContentData.EMPTY.copy(data = pngBytes)
        println(PngDetector.detect(contentBytes, ""))
    }

    @Test
    fun `not a png image`() {
        val bytes = "This is not a png image!".toByteArray()
        val contentBytes = ContentData.EMPTY.copy(data = bytes)
        assertNull(PngDetector.detect(contentBytes, ""))
    }
}
