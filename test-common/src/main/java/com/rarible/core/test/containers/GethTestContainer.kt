package com.rarible.core.test.containers

import io.daonomic.rpc.mono.WebClientTransport
import org.testcontainers.containers.wait.strategy.Wait
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.*
import java.math.BigInteger
import java.net.URI
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

open class GethTestContainer {
    fun ethereumUrl(): URI {
        return URI.create("http://${ethereum.host}:${ethereum.getMappedPort(GETH_WEB_SOCKET_PORT)}")
    }

    fun ethereumWebSocketUrl(): URI {
        return URI.create("ws://${ethereum.host}:${ethereum.getMappedPort(GETH_HTTP_PORT)}")
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
        const val GETH_WEB_SOCKET_PORT: Int = 8546
        const val GETH_HTTP_PORT: Int = 8545

        private val ethereum: KGenericContainer by lazy {
            KGenericContainer("ethereum/client-go:stable").apply {
                withExposedPorts(GETH_WEB_SOCKET_PORT, GETH_HTTP_PORT)
                withCommand("--dev --http --http.api eth,net,web3,debug --http.addr 0.0.0.0 --http.corsdomain '*' --rpcvhosts=* --ws --ws.addr 0.0.0.0 --ws.origins \"*\"")
                waitingFor(Wait.defaultWaitStrategy())
                withReuse(true)
            }
        }

        init {
            ethereum.start()
        }
    }
}

