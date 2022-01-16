package com.rarible.loader.cache.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable auto-configuration for the loader cache infrastructure.
 *
 * Note! Clients need to configure {@link com.rarible.core.loader.configuration.LoadProperties}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(CacheLoaderConfiguration.class)
public @interface EnableRaribleCacheLoader {
}
