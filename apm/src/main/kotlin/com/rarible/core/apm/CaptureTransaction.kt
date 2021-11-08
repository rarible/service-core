package com.rarible.core.apm

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class CaptureTransaction(
    /**
     * The name of the [Transaction].
     * Defaults to the `ClassName#methodName`
     */
    val value: String = ""
)
