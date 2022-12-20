package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.utils.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.database.Database
import org.slf4j.LoggerFactory
import java.time.Instant

private const val schedulerStatePollDelay = 1000L

class ReplayEvents(val arenaEventService: ArenaEventService, val database: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<ReplayEventsTaskData> = Tasks
        .oneTime("replay-events", ReplayEventsTaskData::class.java)
        .execute { instance, context ->
            logger.info("Running task ${instance.taskName}, data: ${instance.data}")

            runBlocking {
                val job = async {
                    arenaEventService.replayEvents(instance.data.table, instance.data.status)
                }

                while (job.isActive) {
                    if (context.schedulerState.isShuttingDown) {
                        logger.info("Stopping task ${instance.taskName} due to shutdown signal")

                        job.cancelAndJoin()

                        logger.info("Task ${instance.taskName} stopped")
                    } else {
                        delay(schedulerStatePollDelay)
                    }
                }
            }
        }

    private val client =
        SchedulerClient.Builder.create(database.getDatasource(), task).serializer(DbSchedulerKotlinSerializer()).build()

    fun schedule(replayEventsTaskData: ReplayEventsTaskData) {
        // Id er alltid det samme slik at bare en instans kan kjøre samtidig
        client.schedule(task.instance("1", replayEventsTaskData), Instant.now())
    }
}

@Serializable
data class ReplayEventsTaskData(
    val table: String?,
    val status: ArenaEvent.ConsumptionStatus?
)
