package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

abstract class OneTimeTaskWrapper<T>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val descriptor: TaskDescriptor<T>

    val task: OneTimeTask<T>
        get() = Tasks
            .oneTime(descriptor)
            .execute { instance, context ->
//                traceExecution(logger, instance) { execute(instance, context) }
                runBlocking { execute(instance, context) }
            }

    abstract suspend fun execute(instance: TaskInstance<T>, context: ExecutionContext)
}

abstract class RecurringTaskWrapper<T>(private val schedule: Schedule) {
    abstract val log: Logger

    abstract val descriptor: TaskDescriptor<T>

    val task: RecurringTask<T>
        get() = Tasks.recurring(descriptor, schedule)
            .execute { instance, context ->
//                traceExecution(logger, instance) { execute(instance, context) }
                runBlocking { execute(instance, context) }
            }

    abstract suspend fun execute(instance: TaskInstance<T>, context: ExecutionContext)
}

fun <T> traceExecution(
    log: Logger,
    instance: TaskInstance<T>,
    execute: suspend () -> Unit,
) {
    val tracer: Tracer = GlobalOpenTelemetry.getTracer("application")

    val span = tracer.spanBuilder(instance.taskName)
        .setAttribute(AttributeKey.stringKey(Attributes.TASK_ID_ATTRIBUTE), instance.id)
        .setAttribute(AttributeKey.stringKey(Attributes.TASK_NAME_ATTRIBUTE), instance.taskName)
        .startSpan()

    try {
        MDC.put(Attributes.TASK_ID_ATTRIBUTE, instance.id)
        MDC.put(Attributes.TASK_NAME_ATTRIBUTE, instance.taskName)

        Context.current().with(span).makeCurrent().use {
            log.info("Task started name=${instance.taskName} id=${instance.id}")

            runBlocking { execute() }

            log.info("Task finished name=${instance.taskName} id=${instance.id}")

            span.setStatus(StatusCode.OK)
        }
    } catch (e: Throwable) {
        log.warn("Task failed name=${instance.taskName} id=${instance.id}")

        span.recordException(e)
        span.setStatus(StatusCode.ERROR)

        throw e
    } finally {
        span.end()

        MDC.remove(Attributes.TASK_ID_ATTRIBUTE)
        MDC.remove(Attributes.TASK_NAME_ATTRIBUTE)
    }
}
