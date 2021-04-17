package com.rarible.core.application

/**
 * Information about environment of a running application.
 *
 * @property name name of the environment, e.g. staging, prod
 * @property host name of host that runs the application
 */
class ApplicationEnvironmentInfo(
    val name: String,
    val host: String
) {
    init {
        require(name.isNotEmpty()) { "Environment name must not be empty" }
        require(host.isNotEmpty()) { "Host name must not be empty" }
    }
}