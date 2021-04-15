package com.rarible.core.mongo.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MongoConfiguration.class)
@EnableReactiveMongoRepositories
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class))
public @interface EnableRaribleMongo {
    @AliasFor(annotation = EnableReactiveMongoRepositories.class, attribute = "basePackageClasses")
    Class<?>[] repositoriesPackage() default {};

    @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanPackage() default {};
}
