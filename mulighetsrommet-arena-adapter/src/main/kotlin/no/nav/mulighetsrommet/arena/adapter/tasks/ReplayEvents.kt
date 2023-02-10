package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.utils.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.database.Database
import org.slf4j.LoggerFactory
import java.time.Instant

private const val SCHEDULER_STATE_POLL_DELAY = 1000L

class ReplayEvents(private val arenaEventService: ArenaEventService, val database: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime("replay-events")
        .execute { instance, context ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val job = async {
                    arenaEventService.retryEvents(status = ArenaEvent.ConsumptionStatus.Replay)
                }

                while (job.isActive) {
                    if (context.schedulerState.isShuttingDown) {
                        logger.info("Stopping task ${instance.taskName} due to shutdown signal")

                        job.cancelAndJoin()

                        logger.info("Task ${instance.taskName} stopped")
                    } else {
                        delay(SCHEDULER_STATE_POLL_DELAY)
                    }
                }
            }
        }

    private val client =
        SchedulerClient.Builder.create(database.getDatasource(), task).build()

    fun schedule() {
        // Id er alltid det samme slik at bare en instans kan kjøre samtidig
        client.schedule(task.instance("1"), Instant.now())
    }
}
