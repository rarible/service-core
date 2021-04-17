package com.rarible.core.telemetry.actuator

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint
import org.springframework.boot.info.GitProperties
import springfox.documentation.annotations.ApiIgnore

@ApiIgnore
@WebEndpoint(id = "version")
internal class VersionWebEndpoint(
    private val gitProperties: GitProperties
) {

    @ReadOperation(produces = ["application/json"])
    fun version(): Map<String, Any?> {
        val result: Map<String, Any?> = mapOf(
            "CommitHash" to gitProperties.commitId
        )

        return result.filterValues { it != null }
    }
}
