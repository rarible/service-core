package com.rarible.core.api

import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.result.condition.PatternsRequestCondition
import org.springframework.web.reactive.result.method.RequestMappingInfo
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPatternParser
import java.lang.IllegalStateException

class RoutesVersionRegister(
    private val oldVersion: String,
    private val currentVersion: String,
    private val currentPathPrefix: String
) : ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val applicationContext = event.applicationContext
        val requestMappingHandlerMapping = applicationContext
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val existingMethods = requestMappingHandlerMapping.handlerMethods
        val urls = ArrayList(existingMethods.keys)
        for (url in urls) {
            for (pattern in url.patternsCondition.patterns) {
                val path = pattern.patternString
                val handlerMethod = existingMethods.get(url)
                if (oldVersion.isNotBlank()) {
                    val newPath = path.replace(currentVersion, oldVersion)
                    if (newPath != path) {
                        val oldPath = createRequestMappingInfo(path.replace(currentVersion, oldVersion), url)
                        registerMappingIfRequired(urls, oldPath, requestMappingHandlerMapping, handlerMethod)
                    }
                }
                val newPath = path.replace(currentPathPrefix, "")
                if (newPath != path) {
                    val unprefixedPath = createRequestMappingInfo(path.replace(currentPathPrefix, ""), url)
                    registerMappingIfRequired(urls, unprefixedPath, requestMappingHandlerMapping, handlerMethod)
                }
            }
        }
    }

    private fun registerMappingIfRequired(
        urls: ArrayList<RequestMappingInfo>,
        mapping: RequestMappingInfo,
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        handlerMethod: HandlerMethod?
    ) {
        if (!urls.contains(mapping)) {
            try {
                requestMappingHandlerMapping.registerMapping(
                    mapping,
                    handlerMethod!!.bean,
                    handlerMethod.method
                )
            } catch (_: IllegalStateException) {
            }
        }
    }

    private fun createRequestMappingInfo(path: String, url: RequestMappingInfo): RequestMappingInfo {
        return RequestMappingInfo(
            PatternsRequestCondition(PathPatternParser.defaultInstance.parse(path)),
            url.methodsCondition,
            url.paramsCondition,
            url.headersCondition,
            url.consumesCondition,
            url.producesCondition,
            url.customCondition
        )
    }
}
