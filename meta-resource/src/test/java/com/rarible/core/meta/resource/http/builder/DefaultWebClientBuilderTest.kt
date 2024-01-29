package com.rarible.core.meta.resource.http.builder

import com.rarible.core.meta.resource.http.DefaultHttpClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

// @Disabled // Run manually.
internal class DefaultWebClientBuilderTest {

    @Test
    fun `allow insecure`() = runBlocking<Unit> {
        val defaultWebClientBuilder = DefaultWebClientBuilder(
            followRedirect = true,
            defaultHeaders = HttpHeaders(),
            insecure = true,
        )
        val client = DefaultHttpClient(
            builder = defaultWebClientBuilder,
            requestTimeout = 30000,
        )

        val body = client.client.get().uri("https://chameleoncollective.io/metadata/9277.json").awaitExchange {
            it.awaitBody<String>()
        }

        assertThat(body).isEqualTo("""{"image":"https://chameleoncollective.io/metadata2/1176.png","attributes":[{"value":"Blue Veins","trait_type":"Eye"},{"value":"Red","trait_type":"Background"},{"value":"Green Moustache","trait_type":"Prop"},{"value":"Tangled White","trait_type":"Mouth"},{"value":"White Hoodie","trait_type":"Clothes"},{"value":"Basketball Green","trait_type":"Tail"},{"value":"Cowboy Brown","trait_type":"Hat"},{"value":"Green","trait_type":"Body"}]}""")
    }
}
