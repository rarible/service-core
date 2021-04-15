package com.rarible.core.common

import java.util.*

fun <T> Optional<T>.orNull(): T? = this.orElse(null)