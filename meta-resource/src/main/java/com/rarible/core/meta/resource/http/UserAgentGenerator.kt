package com.rarible.core.meta.resource.http

import io.netty.util.internal.ThreadLocalRandom

object UserAgentGenerator {
    private const val version1 = "#version1"
    private const val version2 = "#version2"
    private const val version3 = "#version3"

    private val agents = listOf(
        "Mozilla/$version1 (Macintosh; U; PPC Mac OS X; fr-fr) AppleWebKit/$version2 (KHTML, like Gecko) Safari/$version3",
        "Opera/$version1 (X11; Linux x86_64; U; de) Presto/$version2 Version/$version3"
    )

    fun generateUserAgent(): String {
        val template = agents.random()
        return template
            .replace(version1, randomVersion())
            .replace(version2, randomVersion())
            .replace(version3, randomVersion())
    }

    private fun randomVersion(): String {
        return "${ThreadLocalRandom.current().nextInt(1, 30)}.${
            ThreadLocalRandom.current().nextInt(0, 100)
        }.${ThreadLocalRandom.current().nextInt(0, 200)}"
    }
}
