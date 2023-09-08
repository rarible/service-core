package com.rarible.core.apm

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS
)
annotation class CaptureSpan(
    /**
     * The name of the [Span].
     * Defaults to the `ClassName#methodName`
     */
    val value: String = "",

    val type: String = "",

    val subtype: String = "",

    val action: String = ""
)
