package com.rarible.core.content.meta.loader.ipfs

import java.util.Random

interface GatewayResolver {

    fun getGateway(gateways: List<String> = emptyList()): String

    fun getGateways(gateways: List<String> = emptyList()): List<String>
}

open class RandomGatewayResolver(
    gateways: String,
) : GatewayResolver {

    val predefinedGateways = gateways.split(",").map { it.trimEnd('/').trim() }

    override fun getGateway(gateways: List<String>) : String =
        getRandomGateway(gateways.ifEmpty { predefinedGateways })

    override fun getGateways(gateways: List<String>): List<String> {
        return gateways.ifEmpty { predefinedGateways }
    }

    companion object {
        private fun getRandomGateway(gateways: List<String>): String = gateways[Random().nextInt(gateways.size)]
    }
}

