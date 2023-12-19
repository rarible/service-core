package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class TouchLivenessHealthIndicator(
    private val allowedDowntime: Duration,
    private val enabled: Boolean
) : LivenessHealthIndicator {
    private val lastTouch = AtomicReference(Instant.now())

    override fun health(): Health {
        if (enabled) {
            return healthForEnabled()
        } else {
            return healthForDisabled()
        }
    }

    private fun healthForEnabled(): Health {
        val lastTouch = lastTouch.get()
        val status = if (Instant.now() > lastTouch + allowedDowntime) Status.DOWN else Status.UP

        return Health.status(status)
            .withDetail(LAST_TOUCH, lastTouch)
            .withDetail(ALLOWED_DOWN_TIME, allowedDowntime)
            .build()
    }

    private fun healthForDisabled(): Health {
        val lastTouch = lastTouch.get()

        return Health.status(Status.OUT_OF_SERVICE)
            .withDetail(LAST_TOUCH, lastTouch)
            .withDetail(ALLOWED_DOWN_TIME, maxDuration)
            .build()
    }

    override fun up() {
        lastTouch.set(Instant.now())
    }

    override fun down() {
    }

    private companion object {
        val maxDuration = Duration.ofDays(365 * 10)

        const val LAST_TOUCH = "lastTouch"
        const val ALLOWED_DOWN_TIME = "allowedDowntime"
    }
}
