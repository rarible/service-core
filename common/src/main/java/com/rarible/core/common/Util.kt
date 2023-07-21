package com.rarible.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.slf4j.MDCContext

@Suppress("DeferredIsResult")
fun <T> CoroutineScope.asyncWithTraceId(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    context: kotlin.coroutines.CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = async(context = MDCContext() + context, start = start, block = block)

fun CoroutineScope.launchWithTraceId(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context = MDCContext(), start = start, block = block)