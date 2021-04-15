package com.rarible.core.mongo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(PersistPropertiesConfiguration.class)
public @interface IncludePersistProperties {
}

@Configuration
@PropertySource(value = {"classpath:/persist.properties", "classpath:/auto-create-indexes.properties", "classpath:/persist-test.properties", "file:../conf/persist.properties"}, ignoreResourceNotFound = true)
class PersistPropertiesConfiguration {
}
