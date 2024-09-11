package com.rarible.core.telemetry.actuator

import ch.sbb.esta.openshift.gracefullshutdown.IProbeController
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.server.ResponseStatusException
import springfox.documentation.annotations.ApiIgnore

@ApiIgnore
@WebEndpoint(id = "ping")
internal class StatusWebEndpoint : IProbeController {
    @Volatile
    private var ready: Boolean = true

    @ReadOperation(produces = ["application/json"])
    fun ping(): Map<String, Any> {
        if (!ready) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Shutting down")
        }
        return mapOf(
            "status" to "ok"
        )
    }

    override fun setReady(ready: Boolean) {
        this.ready = ready
    }
}
