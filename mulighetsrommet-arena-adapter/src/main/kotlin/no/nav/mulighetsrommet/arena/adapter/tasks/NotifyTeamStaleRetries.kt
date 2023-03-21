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
import java.time.LocalTime

class NotifyTeamStaleRetries(
    private val arenaEventService: ArenaEventService,
    val database: Database,
    private val slackNotifier: SlackNotifier
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val taskName = "notify-team-stale-retries"

    val task: RecurringTask<Void> = Tasks
        .recurring(taskName, Schedules.daily(LocalTime.of(7, 0)))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke kjøre task '$taskName'. Konsekvensen er at man ikke får gitt beskjed på Slack dersom det finnes events som er stale etter for mange retries.")
        }
        .execute { instance, _ ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val retries = 4
                val staleEvents: List<ArenaEvent> = arenaEventService.getStaleEvents(retriesGreaterThan = retries)
                if (staleEvents.isNotEmpty()) {
                    val message = """
                        Det finnes ${staleEvents.size} rader i tabellen 'arena_events' som har retries > $retries.
                        Det gjelder følgende rader: \n
                        ${staleEvents.formaterSlackMeldingForEvents()}
                    """.trimIndent()
                    slackNotifier.sendMessage(message)
                }
            }
        }

    private fun List<ArenaEvent>.formaterSlackMeldingForEvents(): String {
        return this.joinToString("\n") {
            """
                arena-table: ${it.arenaTable}, arena_id: ${it.arenaId}, processing_status: ${it.status.name}, retries: ${it.retries}, message: ${it.message}, operation: ${it.operation.name}
            """.trimIndent()
        }
    }
}
