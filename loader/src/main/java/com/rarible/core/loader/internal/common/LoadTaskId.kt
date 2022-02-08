package com.rarible.core.loader.internal.common

import org.bson.types.ObjectId

typealias LoadTaskId = String

// For very high intensive applications it is better to use UUID (version 1) instead.
internal fun generateLoadTaskId(): LoadTaskId = ObjectId.get().toHexString()
