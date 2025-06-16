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
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

class OpenTelemetrySchedulerListener : SchedulerListener {
    private val log = LoggerFactory.getLogger(javaClass)

    private val executionContexts = ConcurrentHashMap<IdentifiedTask, ExecutionContext>()

    override fun onExecutionStart(currentlyExecuting: CurrentlyExecuting) {
        val task = IdentifiedTask.fromExecution(currentlyExecuting.execution)

        val tracer: Tracer = GlobalOpenTelemetry.getTracer("application")

        val span = tracer.spanBuilder(task.name)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_ID_ATTRIBUTE), task.id)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_NAME_ATTRIBUTE), task.name)
            .startSpan()

        val scope = Context.current().with(span).makeCurrent()

        if (executionContexts.containsKey(task)) {
            log.warn("Found existing execution context for task=$task. This should not happen.")
        }
        executionContexts[task] = ExecutionContext(span, scope)

        log.info("Task started task=$task")
    }

    override fun onExecutionComplete(executionComplete: ExecutionComplete) {
        val task = IdentifiedTask.fromExecution(executionComplete.execution)
        log.info("Task finished task=$task")

        val context = executionContexts.remove(task)
        if (context == null) {
            log.warn("Expected to find an execution context for task=$task. Execution context was not found.")
            return
        }

        context.scope.close()

        try {
            if (executionComplete.result == ExecutionComplete.Result.FAILED) {
                context.span.setStatus(StatusCode.ERROR)
                executionComplete.cause.getOrNull()?.also {
                    context.span.recordException(it)
                }
            } else {
                context.span.setStatus(StatusCode.OK)
            }
        } finally {
            context.span.end()
        }
    }

    override fun onExecutionScheduled(taskInstanceId: TaskInstanceId, executionTime: Instant) = Unit

    override fun onExecutionDead(execution: Execution) = Unit

    override fun onExecutionFailedHeartbeat(currentlyExecuting: CurrentlyExecuting) = Unit

    override fun onSchedulerEvent(type: SchedulerListener.SchedulerEventType) = Unit

    override fun onCandidateEvent(type: SchedulerListener.CandidateEventType) = Unit
}

private data class IdentifiedTask(val name: String, val id: String) {
    companion object {
        fun fromExecution(execution: Execution) = IdentifiedTask(
            name = execution.taskName,
            id = execution.id,
        )
    }
}

private data class ExecutionContext(val span: Span, val scope: Scope)
