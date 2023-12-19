package com.rarible.core.daemon.healthcheck

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class DowntimeLivenessHealthIndicator(
    private val allowedDowntime: Duration
) : LivenessHealthIndicator {

    private val lastDowntime = AtomicReference<Instant?>(null)

    override fun health(): Health {
        val lastDowntime = lastDowntime.get()

        val status = lastDowntime
            ?.let { if (Instant.now() > it + allowedDowntime) Status.DOWN else Status.UP }
            ?: Status.UP

        return Health.status(status).run {
            lastDowntime?.let { withDetail(LivenessHealthIndicator.LAST_DOWNTIME, it) }
            withDetail(LivenessHealthIndicator.ALLOWED_DOWN_TIME, allowedDowntime)
            build()
        }
    }

    override fun up() {
        lastDowntime.set(null)
    }

    override fun down() {
        lastDowntime.set(Instant.now())
    }
}
