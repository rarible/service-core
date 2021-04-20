package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class TouchLivenessHealthIndicator(private val allowedDowntime: Duration) : LivenessHealthIndicator {
    private val lastTouch = AtomicReference(Instant.now())

    override fun health(): Health {
        val lastTouch = lastTouch.get()
        val status = if (Instant.now() > lastTouch + allowedDowntime) Status.DOWN else Status.UP

        return Health.status(status)
            .withDetail(LAST_TOUCH, lastTouch)
            .withDetail(ALLOWED_DOWN_TIME, allowedDowntime)
            .build()
    }

    override fun up() {
        lastTouch.set(Instant.now())
    }

    override fun down() {
    }

    private companion object {
        const val LAST_TOUCH = "lastTouch"
        const val ALLOWED_DOWN_TIME = "allowedDowntime"
    }
}
