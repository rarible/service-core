package com.rarible.core.test.ext

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(ExternalServiceTestExtension::class)
annotation class ExternalServiceTest

class ExternalServiceTestExtension : BeforeAllCallback, BeforeEachCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "external.service.url", "http://127.0.0.1:${mockClientServer.localPort}"
        )
    }

    override fun beforeEach(context: ExtensionContext?) {
        mockClientServer.reset()
    }

    companion object {
        val mockClientServer: ClientAndServer = ClientAndServer.startClientAndServer()
    }
}
