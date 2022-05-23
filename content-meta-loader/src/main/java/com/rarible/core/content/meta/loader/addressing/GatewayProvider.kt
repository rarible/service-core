package com.rarible.core.content.meta.loader.addressing

import java.util.*

interface GatewayProvider {
    fun getGateway(): String

    // TODO remove
    fun getAllGateways(): List<String>
}

class RandomGatewayProvider(
    private val gateways: List<String>
) : GatewayProvider {

    override fun getGateway(): String = gateways[Random().nextInt(gateways.size)]

    override fun getAllGateways(): List<String> = gateways
}

class ConstantGatewayProvider(
    private val gateway: String
) : GatewayProvider {

    override fun getGateway(): String = gateway

    override fun getAllGateways(): List<String> = listOf(gateway)
}

