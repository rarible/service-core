package com.rarible.core.content.meta.loader.detector

import com.rarible.core.content.meta.loader.ContentBytes
import com.rarible.core.content.meta.loader.ContentMeta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PngDetectorTest {
    @Test
    fun `the image is of apng type`() {
        val apngBytes = PngDetectorTest::class.java.classLoader.getResource("btc.png")?.readBytes()
            ?: throw IllegalStateException("Could not find apng file")
        val contentBytes = ContentBytes.EMPTY.copy(bytes = apngBytes)
        val expectedApng = ContentMeta(type = "image/apng", width = 64, height = 64)
        assertEquals(expectedApng, PngDetector.detect(contentBytes))
    }

    @Test
    fun `the image is of png type`() {
        val pngBytes = PngDetectorTest::class.java.classLoader.getResource("btc-icon.png")?.readBytes()
            ?: throw IllegalStateException("Could not find png file")
        val contentBytes = ContentBytes.EMPTY.copy(bytes = pngBytes)
        val expectedApng = ContentMeta(type = "image/png", width = 64, height = 64)
        assertEquals(expectedApng, PngDetector.detect(contentBytes))
    }

    @Test
    fun `not a png image`() {
        val bytes = "This is not a png image!".toByteArray()
        val contentBytes = ContentBytes.EMPTY.copy(bytes = bytes)
        assertNull(PngDetector.detect(contentBytes))
    }
}