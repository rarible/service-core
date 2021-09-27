package com.rarible.core.test.containers

import io.daonomic.rpc.mono.WebClientTransport
import org.testcontainers.containers.wait.strategy.Wait
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.*
import java.math.BigInteger
import java.net.URI
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class OpenEthereumTestContainer {
    fun ethereumUrl(): URI {
        return URI.create("http://${ethereum.host}:${ethereum.getMappedPort(OPEN_ETHEREUM_HTTP_PORT)}")
    }

    fun ethereumWebSocketUrl(): URI {
        return URI.create("ws://${ethereum.host}:${ethereum.getMappedPort(OPEN_ETHEREUM_WEB_SOCKET_PORT)}")
    }

    fun readOnlyTransactionSender(from: Address = Address.ZERO()): ReadOnlyMonoTransactionSender {
        return ReadOnlyMonoTransactionSender(ethereum(), from)
    }

    fun  monoTransactionPoller(): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum())
    }

    fun signingTransactionSender(): MonoSigningTransactionSender {
        val byteArray = ByteArray(32)
        ThreadLocalRandom.current().nextBytes(byteArray)
        val privateKey = Numeric.toBigInt(byteArray)

        val targetEthereum = ethereum()
        return MonoSigningTransactionSender(
            targetEthereum,
            MonoSimpleNonceProvider(targetEthereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
    }

    fun ethereum(): MonoEthereum {
        val transport = object : WebClientTransport(
            ethereumUrl().toASCIIString(),
            MonoEthereum.mapper(),
            Duration.ofSeconds(30).toMillis().toInt(),
            Duration.ofSeconds(30).toMillis().toInt()
        ) {
            override fun maxInMemorySize(): Int = 50000
        }
        return MonoEthereum(transport)
    }

    companion object {
        const val OPEN_ETHEREUM_WEB_SOCKET_PORT: Int = 8546
        const val OPEN_ETHEREUM_HTTP_PORT: Int = 8545

        private val ethereum: KGenericContainer by lazy {
            KGenericContainer("rarible/openethereum:1.0.59").apply {
                withExposedPorts(OPEN_ETHEREUM_HTTP_PORT, OPEN_ETHEREUM_WEB_SOCKET_PORT)
                withCommand("--network-id 18 --chain /home/openethereum/.local/share/config/openethereum/chain.json --tracing on --jsonrpc-interface all --unsafe-expose")
                waitingFor(Wait.defaultWaitStrategy())
                withReuse(true)
            }
        }

        init {
            ethereum.start()
        }
    }
}

