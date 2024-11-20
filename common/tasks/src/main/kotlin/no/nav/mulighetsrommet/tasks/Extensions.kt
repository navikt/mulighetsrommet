package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking

/**
 * Gjør det litt enklere å kjøre db-scheduler tasks med kotlin coroutines.
 */
fun interface SuspendExecutionHandler<T> {
    suspend fun execute(taskInstance: TaskInstance<T>, executionContext: ExecutionContext)
}

fun <T> Tasks.RecurringTaskBuilder<T>.executeSuspend(executionHandler: SuspendExecutionHandler<T>): RecurringTask<T> {
    return execute { taskInstance, executionContext ->
        runBlocking {
            executionHandler.execute(taskInstance, executionContext)
        }
    }
}

fun <T> Tasks.OneTimeTaskBuilder<T>.executeSuspend(executionHandler: SuspendExecutionHandler<T>): OneTimeTask<T> {
    return execute { taskInstance, executionContext ->
        runBlocking {
            executionHandler.execute(taskInstance, executionContext)
        }
    }
}
