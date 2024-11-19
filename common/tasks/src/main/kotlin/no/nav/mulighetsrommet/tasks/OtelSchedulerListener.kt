package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.CurrentlyExecuting
import com.github.kagkarlsson.scheduler.event.SchedulerListener
import com.github.kagkarlsson.scheduler.task.Execution
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

class OtelSchedulerListener : SchedulerListener {
    private val log = LoggerFactory.getLogger(javaClass)

    private var executionSpan: Span? = null
    private var executionScope: Scope? = null

    override fun onExecutionScheduled(taskInstanceId: TaskInstanceId, executionTime: Instant) = Unit

    override fun onExecutionStart(currentlyExecuting: CurrentlyExecuting) {
        val taskName = currentlyExecuting.execution.taskName
        val id = currentlyExecuting.execution.id

        val tracer: Tracer = GlobalOpenTelemetry.getTracer("application")

        val span = tracer.spanBuilder(taskName)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_ID_ATTRIBUTE), id)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_NAME_ATTRIBUTE), taskName)
            .startSpan()

        this.executionSpan = span
        this.executionScope = Context.current().with(span).makeCurrent()

        MDC.put(Attributes.TASK_ID_ATTRIBUTE, id)
        MDC.put(Attributes.TASK_NAME_ATTRIBUTE, taskName)

        log.info("Task started name=$taskName id=$id")
    }

    override fun onExecutionComplete(execution: ExecutionComplete) {
        val taskName = execution.execution.taskName
        val id = execution.execution.id
        log.info("Task finished name=$taskName id=$id")

        MDC.remove(Attributes.TASK_ID_ATTRIBUTE)
        MDC.remove(Attributes.TASK_NAME_ATTRIBUTE)

        executionScope?.close()

        val span = executionSpan ?: return

        when (execution.result) {
            ExecutionComplete.Result.FAILED -> {
                span.setStatus(StatusCode.ERROR)
                execution.cause.getOrNull()?.also {
                    span.recordException(it)
                }
            }

            else -> {
                span.setStatus(StatusCode.OK)
            }
        }

        span.end()
    }

    override fun onExecutionDead(execution: Execution) = Unit

    override fun onExecutionFailedHeartbeat(currentlyExecuting: CurrentlyExecuting) = Unit

    override fun onSchedulerEvent(type: SchedulerListener.SchedulerEventType) = Unit

    override fun onCandidateEvent(type: SchedulerListener.CandidateEventType?) = Unit
}
