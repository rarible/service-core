package com.rarible.loader.cache.configuration

import com.rarible.core.loader.LoadService
import com.rarible.core.loader.configuration.EnableRaribleLoader
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheLoaderEventListener
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheLoaderServiceRegistry
import com.rarible.loader.cache.CacheType
import com.rarible.loader.cache.internal.CacheLoadTaskIdService
import com.rarible.loader.cache.internal.CacheLoaderNotificationListener
import com.rarible.loader.cache.internal.CacheLoaderRunner
import com.rarible.loader.cache.internal.CacheLoaderServiceImpl
import com.rarible.loader.cache.internal.CacheLoaderServiceRegistryImpl
import com.rarible.loader.cache.internal.CacheRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.time.Clock

@Configuration
@EnableRaribleLoader
@EnableReactiveMongoRepositories(basePackageClasses = [CacheLoaderService::class])
@ComponentScan(basePackageClasses = [CacheLoaderService::class])
@EnableConfigurationProperties(CacheLoaderProperties::class)
class CacheLoaderConfiguration {

    @Bean
    fun cacheLoaderRunners(
        cacheLoaders: List<CacheLoader<*>>,
        cacheRepository: CacheRepository,
        clock: Clock
    ): List<CacheLoaderRunner<*>> =
        cacheLoaders.map { cacheLoader ->
            createRunner<Any>(cacheLoader, cacheRepository, clock)
        }

    @Bean
    fun cacheLoaderServices(
        cacheRepository: CacheRepository,
        loadService: LoadService,
        cacheLoadTaskIdService: CacheLoadTaskIdService,
        cacheLoaders: List<CacheLoader<*>>,
        cacheLoaderEventListeners: List<CacheLoaderEventListener<*>>
    ): List<CacheLoaderService<*>> =
        cacheLoaders.map { it.type }.map { cacheType ->
            val cacheLoaderEventListener = getCacheLoaderEventListener<Any>(cacheLoaderEventListeners, cacheType)
            CacheLoaderServiceImpl<Any>(
                type = cacheType,
                cacheRepository = cacheRepository,
                loadService = loadService,
                cacheLoadTaskIdService = cacheLoadTaskIdService,
                cacheLoaderEventListener = cacheLoaderEventListener
            )
        }

    @Bean
    fun cacheLoaderNotificationsListeners(
        cacheLoaderEventListeners: List<CacheLoaderEventListener<*>>,
        cacheLoaderServiceRegistry: CacheLoaderServiceRegistry
    ): List<CacheLoaderNotificationListener<*>> =
        cacheLoaderServiceRegistry.types.map { cacheType ->
            val cacheLoaderEventListener = getCacheLoaderEventListener<Any>(cacheLoaderEventListeners, cacheType)
            val cacheLoaderService = cacheLoaderServiceRegistry.getCacheLoaderService<Any>(cacheType)
            createNotificationListener<Any>(
                type = cacheType,
                cacheLoaderEventListener = cacheLoaderEventListener,
                cacheLoaderService = cacheLoaderService
            )
        }

    @Bean
    fun cacheLoaderServiceRegistry(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderServiceRegistry = CacheLoaderServiceRegistryImpl(cacheLoaderServices)

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCacheLoaderEventListener(
        cacheLoaderEventListeners: List<CacheLoaderEventListener<*>>,
        cacheType: CacheType
    ): CacheLoaderEventListener<T> = cacheLoaderEventListeners.find { it.type == cacheType } as? CacheLoaderEventListener<T>
        ?: throw AssertionError("No associated cache loader listener found for $cacheType")

    @Suppress("UNCHECKED_CAST")
    private fun <T> createNotificationListener(
        type: CacheType,
        cacheLoaderEventListener: CacheLoaderEventListener<*>,
        cacheLoaderService: CacheLoaderService<*>
    ) = CacheLoaderNotificationListener(
        cacheType = type,
        cacheLoaderEventListener = cacheLoaderEventListener as CacheLoaderEventListener<T>,
        cacheLoaderService = cacheLoaderService as CacheLoaderService<T>
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> createRunner(
        cacheLoader: CacheLoader<*>,
        cacheRepository: CacheRepository,
        clock: Clock
    ) = CacheLoaderRunner(
        cacheType = cacheLoader.type,
        cacheLoader = cacheLoader as CacheLoader<T>,
        repository = cacheRepository,
        clock = clock
    )
}
