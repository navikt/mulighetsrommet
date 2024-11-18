package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

abstract class RecurringTaskWrapper<T>(private val schedule: Schedule) {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val descriptor: TaskDescriptor<T>

    val task: RecurringTask<T>
        get() = Tasks.recurring(descriptor, schedule)
            .execute { instance, context ->
                traceExecution(instance, context)
            }

    private fun traceExecution(instance: TaskInstance<T>, context: ExecutionContext) {
        val tracer: Tracer = GlobalOpenTelemetry.getTracer("application")

        val span = tracer.spanBuilder(instance.taskName)
            .setAttribute(AttributeKey.stringKey("task.id"), instance.id)
            .setAttribute(AttributeKey.stringKey("task.name"), instance.taskName)
            .startSpan()

        try {
            Context.current().with(span).makeCurrent().use {
                logger.info("Running task ${instance.taskName} ${instance.id}")
                runBlocking { execute(instance, context) }
                logger.info("Task finished ${instance.taskName} ${instance.id}")

                span.setStatus(StatusCode.OK)
            }
        } catch (e: Throwable) {
            logger.warn("Task failed ${instance.taskName} ${instance.id}")

            span.recordException(e)
            span.setStatus(StatusCode.ERROR)

            throw e
        } finally {
            span.end()
        }
    }

    abstract suspend fun execute(instance: TaskInstance<T>, context: ExecutionContext)
}
