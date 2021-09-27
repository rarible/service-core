package com.rarible.core.task

import org.apache.commons.lang3.exception.ExceptionUtils
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/**
 * NOT A PUBLIC API.
 *
 * Internal state of the running task persisted in the database to allow task resuming.
 */
@Document(collection = "task")
@CompoundIndexes(
    CompoundIndex(def = "{type: 1, param: 1}", background = true, unique = true),
    CompoundIndex(def = "{running: 1, lastStatus: 1}", background = true)
)
data class Task(
    val type: String,
    val param: String,
    val state: Any? = null,
    val running: Boolean,
    val lastStatus: TaskStatus = TaskStatus.NONE,
    val lastError: String? = null,
    val lastFinishDate: Date? = null,
    val lastUpdateDate: Date = Date(),
    val sample: Long? = DEFAULT_SAMPLE,
    @Id
    val id: ObjectId = ObjectId.get(),
    @Version
    val version: Long? = null
) {
    fun withState(newState: Any) = copy(
        state = newState,
        lastUpdateDate = Date()
    )

    fun clearRunning() = copy(
        running = false,
        lastUpdateDate = Date()
    )

    fun withSample(sample: Long?) = copy(
        sample = sample
    )

    fun markRunning() = copy(
        running = true,
        lastUpdateDate = Date()
    )

    fun markCompleted() = copy(
        running = false,
        lastStatus = TaskStatus.COMPLETED,
        lastUpdateDate = Date(),
        lastFinishDate = Date()
    )

    fun markError(error: Throwable) = copy(
        lastStatus = TaskStatus.ERROR,
        running = false,
        lastError = ExceptionUtils.getStackTrace(error),
        lastUpdateDate = Date(),
        lastFinishDate = Date()
    )

    companion object {
        const val DEFAULT_SAMPLE = 5000L
    }
}

enum class TaskStatus {
    NONE,
    ERROR,
    COMPLETED
}
