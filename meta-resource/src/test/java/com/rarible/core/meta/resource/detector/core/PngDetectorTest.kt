package com.rarible.core.meta.resource.detector.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class PngDetectorTest {
    @Test
    fun `the image is of apng type`() {
        val apngBytes = Files.readAllBytes(Paths.get(this::class.java.getResource("/meta/resource/detector/core/btc.png").toURI()))
            ?: throw IllegalStateException("Could not find apng file")

        val contentBytes = ContentBytes.EMPTY.copy(bytes = apngBytes)
        val expectedApng = ContentMeta(type = "image/apng", width = 64, height = 64)
        assertEquals(expectedApng, PngDetector.detect(contentBytes))
    }

    @Test
    fun `the image is of png type`() {
        val pngBytes = Files.readAllBytes(Paths.get(this::class.java.getResource("/meta/resource/detector/core/btc-icon.png").toURI()))
            ?: throw IllegalStateException("Could not find apng file")
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
