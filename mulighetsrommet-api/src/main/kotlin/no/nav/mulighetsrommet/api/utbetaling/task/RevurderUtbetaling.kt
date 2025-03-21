package no.nav.mulighetsrommet.api.utbetaling.task

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.Session
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import java.time.Instant
import java.util.*

class RevurderUtbetaling(
    private val utbetalingService: UtbetalingService,
) {

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val gjennomforingId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .executeSuspend { instance, _ ->
            utbetalingService.revurderUtbetalingForGjennomforing(instance.data.gjennomforingId)
        }

    fun schedule(gjennomforingId: UUID, startTime: Instant, session: Session) {
        val instance = task.instance(gjennomforingId.toString(), TaskData(gjennomforingId))
        val client = transactionalSchedulerClient(task, session.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
    }
}
