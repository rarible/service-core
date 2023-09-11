package com.rarible.core.application

data class ApplicationInfo(
    val serviceName: String,
    val project: String
) {
    init {
        require(project.isNotEmpty()) { "Project name must not be empty" }
        require(serviceName.isNotEmpty()) { "Service name must not be empty" }
    }
}
