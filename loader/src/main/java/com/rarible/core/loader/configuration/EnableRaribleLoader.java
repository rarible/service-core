package com.rarible.core.loader.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable auto-configuration for the loader infrastructure.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LoaderConfiguration.class)
public @interface EnableRaribleLoader {
}
