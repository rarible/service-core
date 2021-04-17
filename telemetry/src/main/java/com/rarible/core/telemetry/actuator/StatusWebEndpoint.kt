package com.rarible.core.telemetry.actuator

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import springfox.documentation.annotations.ApiIgnore

@ApiIgnore
@WebEndpoint(id = "ping")
internal class StatusWebEndpoint {
    @ReadOperation(produces = ["application/json"])
    fun ping(): Map<String, Any> {
        return mapOf(
            "status" to "ok"
        )
    }
}
