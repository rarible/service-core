package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.HealthIndicator

interface LivenessHealthIndicator : HealthIndicator {
    fun up()

    fun down()

    companion object {
        const val LAST_TOUCH = "lastTouch"
        const val LAST_DOWNTIME = "lastDowntime"
        const val ALLOWED_DOWN_TIME = "allowedDowntime"
    }
}
