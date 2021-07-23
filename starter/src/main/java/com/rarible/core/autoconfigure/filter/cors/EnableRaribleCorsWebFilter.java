package com.rarible.core.autoconfigure.filter.cors;

import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@PropertySource(value = "classpath:/rarible-cors-webfilter.properties")
public @interface EnableRaribleCorsWebFilter {
}
