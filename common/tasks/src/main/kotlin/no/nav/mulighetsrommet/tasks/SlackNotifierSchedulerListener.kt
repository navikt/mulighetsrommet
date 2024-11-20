package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.CurrentlyExecuting
import com.github.kagkarlsson.scheduler.event.SchedulerListener
import com.github.kagkarlsson.scheduler.task.Execution
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import io.opentelemetry.api.trace.Span
import no.nav.mulighetsrommet.slack.SlackNotifier
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

class SlackNotifierSchedulerListener(
    private val slack: SlackNotifier,
) : SchedulerListener {

    override fun onExecutionComplete(executionComplete: ExecutionComplete) {
        if (executionComplete.result != ExecutionComplete.Result.FAILED) {
            return
        }

        val spanContext = Span.current().spanContext

        val cause = executionComplete.cause

        slack.sendMessage(
            """
            |:warning: *Skedulert jobb har feilet!* :warning:
            |
            |Noen burde undersøke hva som har gått galt.
            |
            |*Task name:* ${executionComplete.execution.taskName}
            |*Task id:* ${executionComplete.execution.id}
            |*Span id:* ${spanContext.spanId}
            |*Trace id:* ${spanContext.traceId}
            |*Exception message:* ${cause.getOrNull()?.message}
            |*Exception stack trace:* ```${cause.getOrNull()?.stackTraceToString()}```
            """.trimMargin(),
        )
    }

    override fun onExecutionStart(currentlyExecuting: CurrentlyExecuting) = Unit

    override fun onExecutionScheduled(taskInstanceId: TaskInstanceId, executionTime: Instant) = Unit

    override fun onExecutionDead(execution: Execution) = Unit

    override fun onExecutionFailedHeartbeat(currentlyExecuting: CurrentlyExecuting) = Unit

    override fun onSchedulerEvent(type: SchedulerListener.SchedulerEventType) = Unit

    override fun onCandidateEvent(type: SchedulerListener.CandidateEventType) = Unit
}
