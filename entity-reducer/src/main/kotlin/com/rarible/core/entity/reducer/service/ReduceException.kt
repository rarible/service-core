package com.rarible.core.entity.reducer.service

class ReduceException(
    val event: Any?,
    val entity: Any,
    cause: Throwable
) : RuntimeException(cause)
