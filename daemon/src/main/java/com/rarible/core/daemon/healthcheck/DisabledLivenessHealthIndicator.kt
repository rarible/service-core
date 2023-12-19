package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class DisabledLivenessHealthIndicator() : LivenessHealthIndicator {

    private val lastTouch = AtomicReference(Instant.now())

    override fun health(): Health {
        val lastTouch = lastTouch.get()

        return Health.status(Status.OUT_OF_SERVICE)
            .withDetail(LivenessHealthIndicator.LAST_TOUCH, lastTouch)
            .withDetail(LivenessHealthIndicator.ALLOWED_DOWN_TIME, maxDuration)
            .build()
    }

    override fun up() {
        lastTouch.set(Instant.now())
    }

    override fun down() {
    }

    private companion object {
        val maxDuration = Duration.ofDays(365 * 10)
    }
}
