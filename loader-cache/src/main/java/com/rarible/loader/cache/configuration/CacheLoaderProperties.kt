package com.rarible.loader.cache.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.util.concurrent.TimeUnit

@ConfigurationProperties("cache-loader")
@ConstructorBinding
class CacheLoaderProperties

// For now, it is empty, but may be used.
