package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack_notifier.SlackNotifier
import org.slf4j.LoggerFactory

class NotifyFailedEvents(
    private val arenaEventService: ArenaEventService,
    val database: Database,
    private val slackNotifier: SlackNotifier,
    private val config: Config
) {

    data class Config(
        val cron: String,
        val maxRetries: Int
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val taskName = "notify-team-stale-retries"

    val task: RecurringTask<Void> = Tasks
        .recurring(taskName, Schedules.cron(config.cron))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke kjøre task '$taskName'. Konsekvensen er at man ikke får gitt beskjed på Slack dersom det finnes events som er stale etter for mange retries.")
        }
        .execute { instance, _ ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val retries = config.maxRetries
                val staleEvents: List<ArenaEvent> =
                    arenaEventService.getStaleEvents(retriesGreaterThanOrEqual = retries)
                if (staleEvents.isNotEmpty()) {
                    val message = """
                    Det finnes ${staleEvents.size} rader i tabellen 'arena_events' som har retries >= $retries.
                    Det gjelder følgende rader:
                    ${staleEvents.formaterSlackMeldingForEvents()}
                    """.trimIndent()
                    slackNotifier.sendMessage(message)
                }
            }
        }

    private fun List<ArenaEvent>.formaterSlackMeldingForEvents(): String {
        return this.joinToString("\n") {
            """
                arena-table: ${it.arenaTable}, arena_id: ${it.arenaId}
            """.trimIndent()
        }
    }
}
