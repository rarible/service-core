package com.rarible.core.autoconfigure.nginx;

import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@PropertySource(value = "classpath:/rarible-nginx-expose.properties")
public @interface EnableRaribleNginxExpose {
}
