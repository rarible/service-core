package com.rarible.core.meta.resource.detector

import com.google.common.primitives.Bytes

const val SPACE_CODE = "%20"
const val SVG_TAG = "<svg"
private val SVG_TAG_BYTES = SVG_TAG.toByteArray(Charsets.UTF_8) // Tag could contains something like <svg a='b'>

fun containsSvgTag(bytes: ByteArray): Boolean = Bytes.indexOf(bytes, SVG_TAG_BYTES) >= 0

fun containsSvgTag(url: String): Boolean = url.indexOf(SVG_TAG) >= 0

