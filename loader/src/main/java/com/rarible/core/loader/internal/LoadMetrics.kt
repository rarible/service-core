package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class LoadMetrics(
    private val meterRegistry: MeterRegistry
) {
    private val loaderTimers = ConcurrentHashMap<Pair<LoadType, Boolean /* success */>, Timer>()
    private val loaderScheduledTasksCounter = ConcurrentHashMap<Pair<LoadType, /* forRetry */ Boolean>, Counter>()

    private fun getLoaderTimer(type: LoadType, success: Boolean): Timer =
        loaderTimers.compute(type to success) { _, _ ->
            meterRegistry.timer(
                LOADER_TIMER,
                "type",
                type,
                "success",
                success.booleanToString()
            )
        }!!

    private fun getScheduledTasksCounter(type: LoadType, forRetry: Boolean): Counter =
        loaderScheduledTasksCounter.compute(type to forRetry) { _, _ ->
            meterRegistry.counter(
                LOADER_SCHEDULED_TASKS_COUNTER,
                "type",
                type,
                "forRetry",
                forRetry.booleanToString()
            )
        }!!

    private fun Boolean.booleanToString() = if (this) "true" else "false"

    fun onTaskScheduled(type: LoadType, forRetry: Boolean) {
        getScheduledTasksCounter(type, forRetry).increment()
    }

    fun onLoaderStarted(): Timer.Sample =
        Timer.start(meterRegistry)

    fun onLoaderSuccess(type: LoadType, sample: Timer.Sample) {
        sample.stop(getLoaderTimer(type, true))
    }

    fun onLoaderFailed(type: LoadType, sample: Timer.Sample) {
        sample.stop(getLoaderTimer(type, false))
    }

    fun getNumberOfScheduledTasks(type: LoadType, forRetry: Boolean): Long =
        getScheduledTasksCounter(type, forRetry).count().toLong()

    fun getNumberOfFinishedTasks(type: LoadType, success: Boolean): Long =
        getLoaderTimer(type, success).count()

    fun reset() {
        meterRegistry.clear()
    }

    private companion object {
        const val LOADER_TIMER = "loader_time"
        const val LOADER_SCHEDULED_TASKS_COUNTER = "loader_scheduled_tasks"
    }
}
