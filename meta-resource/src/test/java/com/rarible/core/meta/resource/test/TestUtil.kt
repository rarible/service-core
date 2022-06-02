package com.rarible.core.meta.resource.test

import java.nio.file.Files
import java.nio.file.Paths

fun readFile(path: String) =
    Files.readAllBytes(
        Paths.get(ResourceTestData::class.java.getResource(path).toURI())
    )
