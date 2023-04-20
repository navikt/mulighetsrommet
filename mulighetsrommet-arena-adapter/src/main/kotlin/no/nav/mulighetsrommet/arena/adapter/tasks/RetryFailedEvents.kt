package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class RetryFailedEvents(
    private val config: Config,
    private val arenaEventService: ArenaEventService,
    private val slackNotifier: SlackNotifier,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val schedulerStatePollDelay: Long = 1000,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("retry-failed-events", FixedDelay.ofMinutes(config.delayOfMinutes))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke retry'e feilede hendelser i arena-adapter. En utvikler bør se på hvorfor.")
        }
        .execute { instance, context ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val job = async {
                    arenaEventService.retryEvents(status = ArenaEvent.ProcessingStatus.Failed)
                }

                while (job.isActive) {
                    if (context.schedulerState.isShuttingDown) {
                        logger.info("Stopping task ${instance.taskName} due to shutdown signal")

                        job.cancelAndJoin()

                        logger.info("Task ${instance.taskName} stopped")
                    } else {
                        delay(config.schedulerStatePollDelay)
                    }
                }
            }
        }
}
