package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.HealthIndicator

interface LivenessHealthIndicator : HealthIndicator {
    fun up()

    fun down()
}