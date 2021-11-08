package com.rarible.core.apm

import co.elastic.apm.api.Span
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ApmContext(val span: Span) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ApmContext>
}
