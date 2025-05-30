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

    private val executionContexts = ConcurrentHashMap<String, ExecutionContext>()

    private data class ExecutionContext(val span: Span, val scope: Scope)

    override fun onExecutionStart(currentlyExecuting: CurrentlyExecuting) {
        val executionId = currentlyExecuting.execution.id
        val taskName = currentlyExecuting.execution.taskName

        val tracer: Tracer = GlobalOpenTelemetry.getTracer("application")

        val span = tracer.spanBuilder(taskName)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_ID_ATTRIBUTE), executionId)
            .setAttribute(AttributeKey.stringKey(Attributes.TASK_NAME_ATTRIBUTE), taskName)
            .startSpan()

        val scope = Context.current().with(span).makeCurrent()

        if (executionContexts.containsKey(executionId)) {
            log.warn("Found existing execution context for task name=$taskName id=$executionId. This should not happen.")
        }
        executionContexts[executionId] = ExecutionContext(span, scope)

        log.info("Task started name=$taskName id=$executionId")
    }

    override fun onExecutionComplete(executionComplete: ExecutionComplete) {
        val executionId = executionComplete.execution.id
        val taskName = executionComplete.execution.taskName
        log.info("Task finished name=$taskName id=$executionId")

        val context = executionContexts.remove(executionId)
        if (context == null) {
            log.warn("Expected to find an execution context for task name=$taskName id=$executionId. Execution context was not found.")
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
