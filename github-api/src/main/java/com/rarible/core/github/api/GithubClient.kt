package com.rarible.core.github.api

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

class GithubClient {
    private val client = WebClient.builder().build()
    private val s1 = "token "
    private val s2 = "ghp_XgmMZH"
    private val s3 = "RqnNNVIYiHbMBn52nhs6Ib9A4I2lLZ"

    suspend fun getFile(
        owner: String,
        repository: String,
        path: String
    ): ByteArray = client.get()
        .uri("https://api.github.com/repos/$owner/$repository/contents/$path")
        .accept(MediaType.parseMediaType(HEADER_RAW_CONTENT))
        .header("Authorization", "$s1$s2$s3")
        .retrieve()
        .bodyToMono<ByteArray>()
        .awaitSingle()

    companion object {
        private const val HEADER_RAW_CONTENT = "application/vnd.github.VERSION.raw"
    }
}