package com.rarible.core.meta.resource.resolver

import java.util.Random

interface GatewayProvider {
    fun getGateway(): String
}

class RandomGatewayProvider(
    private val gateways: List<String>
) : GatewayProvider {

    override fun getGateway(): String = gateways[Random().nextInt(gateways.size)]
}

class ConstantGatewayProvider(
    private val gateway: String
) : GatewayProvider {

    override fun getGateway(): String = gateway
}
