package com.rarible.core.common

import org.springframework.core.convert.ConversionService

inline fun <reified T> ConversionService.convert(source: Any): T {
    return this.convert(source, T::class.java) ?: error("Can't convert ${source.javaClass} to ${T::class.java}")
}
