package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.MongoLoadTaskRepository
import com.rarible.core.test.data.randomBoolean
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.OptimisticLockingFailureException

class LoadTaskServiceIt : AbstractIntegrationTest() {
    @Test
    fun `mongo indexes`() = runBlocking<Unit> {
        val indexInfos = mongo.indexOps(MongoLoadTaskRepository.COLLECTION).indexInfo.asFlow().toSet()
        assertThat(indexInfos.map { it.name }.toSet())
            .isEqualTo(
                setOf(
                    "_id_",
                    "status.rescheduled_1_status.retryAt_1__id_1",
                    "status._class_1__id_1",
                    "status.scheduledAt_1__id_1",
                    "type_1_key_1__id_1"
                )
            )
    }

    @Test
    fun `save and get`() = runBlocking<Unit> {
        assertThat(loadTaskService.getAll().toList()).isEmpty()
        val loadTask = save(randomLoadTask(status = randomScheduledStatus()))
        assertThat(loadTaskService.get(loadTask.id)).isEqualTo(loadTask)
        assertThat(loadTaskService.getAll().toList()).isEqualTo(listOf(loadTask))
    }

    @Test
    fun update() = runBlocking<Unit> {
        val loadTask1 = save(randomLoadTask(status = randomScheduledStatus()))
        assertThat(loadTaskService.get(loadTask1.id)).isEqualTo(loadTask1)
        val loadTask2 = save(loadTask1.copy(status = randomLoadedStatus()))
        assertThat(loadTaskService.get(loadTask2.id)).isEqualTo(loadTask2)
    }

    @Test
    fun `if version differs throw OptimisticLockingFailureException`() = runBlocking<Unit> {
        val loadTask1 = save(randomLoadTask(status = randomScheduledStatus()))
        assertThat(loadTaskService.get(loadTask1.id)).isEqualTo(loadTask1)
        val loadTask2 = loadTask1.copy(status = randomLoadedStatus(), version = loadTask1.version + 42)
        assertThrows<OptimisticLockingFailureException> {
            save(loadTask2)
        }
    }

    @Test
    fun remove() = runBlocking<Unit> {
        val toSaveTask = randomLoadTask(status = randomScheduledStatus())
        assertThat(loadTaskService.get(toSaveTask.id)).isNull()
        val loadTask1 = save(toSaveTask)
        assertThat(loadTaskService.get(loadTask1.id)).isEqualTo(loadTask1)
    }

    @Test
    fun `find tasks to retry - only return with status WaitsForRetry`() = runBlocking<Unit> {
        // Ignore these tasks.
        save(randomLoadTask(status = randomScheduledStatus()))
        save(randomLoadTask(status = randomLoadedStatus()))
        save(randomLoadTask(status = randomFailedStatus()))

        val waitsForRetryStatus = randomWaitsForRetryStatus().copy(rescheduled = false)
        val waitsForRetryTask = save(randomLoadTask(status = waitsForRetryStatus))
        assertThat(loadTaskService.getTasksToRetry(waitsForRetryStatus.retryAt.plusSeconds(1)).toList())
            .isEqualTo(listOf(waitsForRetryTask))
    }

    @Test
    fun `find tasks to retry - respect 'rescheduled' flag - and retryAt is less than now`() = runBlocking<Unit> {
        val pivot = nowMillis()
        val allTasksToBeSaved = (-30L..30L).map { timeShift ->
            val waitsForRetryStatus = randomWaitsForRetryStatus()
                .copy(rescheduled = randomBoolean(), retryAt = pivot.plusSeconds(timeShift))
            randomLoadTask(status = waitsForRetryStatus)
        }
        val allTasks = allTasksToBeSaved.shuffled().map { save(it) }
        val expectedTasks = allTasks
            .filter {
                val status = it.status as LoadTask.Status.WaitsForRetry
                status.rescheduled.not() && status.retryAt < pivot
            }
            .sortedBy { (it.status as LoadTask.Status.WaitsForRetry).retryAt }
        assertThat(loadTaskService.getTasksToRetry(pivot).toList()).isEqualTo(expectedTasks)
    }

    private suspend fun save(loadTask: LoadTask): LoadTask {
        val saved = loadTaskService.save(loadTask)
        assertThat(saved).isEqualTo(loadTask.copy(version = loadTask.version + 1))
        return saved
    }

}
