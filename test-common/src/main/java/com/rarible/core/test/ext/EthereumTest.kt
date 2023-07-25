package com.rarible.core.test.ext

import com.rarible.core.test.containers.OpenEthereumTestContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.System.setProperty
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(EthereumTestExtension::class)
annotation class EthereumTest

class EthereumTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        setProperty("rarible.common.parityUrls", ethereumContainer.ethereumUrl().toString())
        setProperty("rarible.common.parityWebSocketUrls", ethereumContainer.ethereumWebSocketUrl().toString())

        //configuration io.daonomic.ethereum.listener.common.EthereumConfiguration has different ethereum prefix
        setProperty("parityUrls", ethereumContainer.ethereumUrl().toString())
        setProperty("parityWebSocketUrls", ethereumContainer.ethereumWebSocketUrl().toString())

        setProperty("rarible.ethereum.nodes[0].httpUrl", ethereumContainer.ethereumUrl().toString())
        setProperty("rarible.ethereum.nodes[0].websocketUrl", ethereumContainer.ethereumWebSocketUrl().toString())
    }

    companion object {
        val ethereumContainer = OpenEthereumTestContainer()
    }
}