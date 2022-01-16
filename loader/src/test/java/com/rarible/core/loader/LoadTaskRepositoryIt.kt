package com.rarible.core.loader

import com.rarible.core.loader.internal.LoadTask
import com.rarible.core.loader.internal.MongoLoadTaskRepository
import com.rarible.core.loader.internal.generateLoadTaskId
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

// TODO[loader]: improve this test finally.
/*
class LoadTaskRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `mongo indexes`() = runBlocking<Unit> {
        val indexInfos = mongo.indexOps(MongoLoadTaskRepository.COLLECTION).indexInfo.asFlow().toSet()
        assertThat(indexInfos.map { it.name }.toSet())
            .isEqualTo(setOf("_id_", "failedAt_1__id_1", "retryAt_1__id_1", "type_1_key_1"))
        assertThat(indexInfos.find { it.name == "type_1_key_1" }?.isUnique).isFalse()
    }

    @Test
    fun `save and test get-getAll-findWithMaxRetryAt`() = runBlocking<Unit> {
        val type = "type"
        val key = "key"
        assertThat(loadTaskService.get(type, key)).isNull()
        assertThat(loadTaskService.getAll().toList()).isEmpty()
        val loadTask = randomLoadTask().copy(type = type, key = key)
        assertThat(loadTaskService.save(loadTask)).isEqualTo(loadTask)
        val allTasks = listOf(loadTask) + (0 until 100L).map { randomLoadTask() }
        allTasks.forEach { assertThat(loadTaskService.save(it)).isEqualTo(it) }
        assertThat(loadTaskService.get(type, key)).isEqualTo(loadTask)
        val comparator = compareBy<LoadTask> { it.failedAt }.thenBy { it.id }
        assertThat(loadTaskService.getAll().toList()).isEqualTo(allTasks.sortedWith(comparator))
    }

    @Test
    fun `find with max retry at - should be sorted by retry at`() = runBlocking<Unit> {
        val loadTasks = (0 until 100L).map { randomLoadTask() } +
                // At least one with 100% non-null retryAt.
                listOf(randomLoadTask().copy(retryAt = randomInstant()))
        loadTasks.forEach { loadTaskService.save(it) }

        val comparator = compareBy<LoadTask> { it.retryAt }.thenBy { it.id }

        val retryableTasks = loadTasks.filter { it.retryAt != null }.sortedWith(comparator)
        val expectedRetryableTasks = retryableTasks.take(20)
        val maxRetryAt = expectedRetryableTasks.last().retryAt!!
        assertThat(loadTaskService.getTasksToRetry(maxRetryAt).toList())
            .isEqualTo(expectedRetryableTasks.dropLast(1))
    }

    @Test
    fun remove() = runBlocking<Unit> {
        val type = "type"
        val key = "key"
        val loadTask = randomLoadTask().copy(type = type, key = key)
        loadTaskService.remove(loadTask)
        loadTaskService.save(loadTask)
        assertThat(loadTaskService.get(type, key)).isEqualTo(loadTask)
        loadTaskService.remove(loadTask)
        assertThat(loadTaskService.get(type, key)).isNull()
    }

    private fun randomLoadTask() = LoadTask(
        id = generateLoadTaskId(),
        type = randomString(),
        key = randomString(),
        attempts = randomInt(5),
        scheduledAt = randomInstant(),
        failedAt = randomInstant(),
        retryAt = if (randomBoolean()) randomInstant() else null
    )

    private fun randomInstant() = Instant.ofEpochSecond(randomLong(1000))
}
*/
