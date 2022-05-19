package com.rarible.core.content.meta.loader.ipfs

import java.util.Random

interface GatewayProvider {
    fun getGateway(): String

    fun getAllGateways(): List<String>
}

class RandomGatewayProvider(
    private val gateways: List<String>
) : GatewayProvider {

    override fun getGateway(): String =
        getRandomGateway(gateways)

    override fun getAllGateways(): List<String> = gateways

    companion object {
        private fun getRandomGateway(gateways: List<String>): String = gateways[Random().nextInt(gateways.size)]
    }
}

class ConstantGatewayProvider(
    private val gateway: String
) : GatewayProvider {

    override fun getGateway(): String = gateway

    override fun getAllGateways(): List<String> = listOf(gateway)
}

