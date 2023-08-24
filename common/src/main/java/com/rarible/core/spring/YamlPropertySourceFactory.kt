package com.rarible.core.spring

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.util.Properties

class YamlPropertySourceFactory : PropertySourceFactory {
    override fun createPropertySource(name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(encodedResource.resource)
        return try {
            val properties: Properties = factory.getObject()!!
            PropertiesPropertySource(encodedResource.resource.filename!!, properties)
        } catch (e: Exception) {
            // If not found
            return MapPropertySource(name, emptyMap())
        }
    }
}
