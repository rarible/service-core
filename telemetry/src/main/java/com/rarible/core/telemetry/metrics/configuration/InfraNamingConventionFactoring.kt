package com.rarible.core.telemetry.metrics.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import io.micrometer.core.instrument.Tag

object InfraNamingConventionFactoring {
    fun getCommonTags(appInfo: ApplicationInfo, envInfo: ApplicationEnvironmentInfo): List<Tag> {
        return listOf(
            Tag.of("project", appInfo.project),
            Tag.of("service", appInfo.serviceName),
            Tag.of("host", envInfo.host),
            Tag.of("env", envInfo.name)
        )
    }
}
