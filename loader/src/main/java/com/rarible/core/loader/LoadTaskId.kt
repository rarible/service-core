package com.rarible.core.loader

import org.bson.types.ObjectId

/**
 * Load task ID used to [track][LoadService.getStatus] status of the loading task.
 */
typealias LoadTaskId = String

/**
 * Generate a unique task ID with which the task
 */
//TODO: For very high performant applications it is better to use UUID (version 1) instead.
fun generateLoadTaskId(): LoadTaskId = ObjectId.get().toHexString()
