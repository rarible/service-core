package com.rarible.core.common

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

fun <T> Mono<T>.retryOptimisticLock(retries: Long = 5): Mono<T> = this
    .retryWhen(Retry.max(retries).filter { it is OptimisticLockingFailureException || it is DuplicateKeyException })
