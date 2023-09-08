package com.rarible.core.telemetry.actuator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange

class WebRequestClientTagContributorTest {

    private val contributor = WebRequestClientTagContributor()

    @Test
    fun `client name taken from header - rarible`() {
        val exchange = createExchange(clientName = "rarible")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("rarible")
    }

    @Test
    fun `client name taken from header - rarible protocol`() {
        val exchange = createExchange(clientName = "rarible-protocol")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("rarible-protocol")
    }

    @Test
    fun `client name taken from origin`() {
        val exchange = createExchange(origin = "https://dev.rarible.com")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("rarible")
    }

    @Test
    fun `client name without server exchange`() {
        val tag = contributor.httpRequestTags(null, null)!!.first()
        assertThat(tag.value).isEqualTo("other")
    }

    @Test
    fun `client name filtered from header`() {
        val exchange = createExchange(clientName = "not-rarible")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("other")
    }

    @Test
    fun `client name filtered from origin`() {
        val exchange = createExchange(origin = "https://something.com")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("other")
    }

    @Test
    fun `client name filtered from header, origin not checked`() {
        // Origin can be parsed to clientName,
        // but since there is non-applicable clientName in header, it should be omitted
        val exchange = createExchange(origin = "https://rarible.com", clientName = "not-rarible")
        val tag = contributor.httpRequestTags(exchange, null)!!.first()
        assertThat(tag.value).isEqualTo("other")
    }

    private fun createExchange(
        origin: String? = null,
        clientName: String? = null
    ): ServerWebExchange {
        val request = MockServerHttpRequest.get("/")
        origin?.let { request.header(HttpHeaders.ORIGIN, origin) }
        clientName?.let { request.header("x-rarible-client", clientName) }
        return MockServerWebExchange.builder(request.build()).build()
    }
}
