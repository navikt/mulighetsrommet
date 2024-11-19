package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier

class NotifyFailedEvents(
    private val arenaEventService: ArenaEventService,
    val database: Database,
    private val slackNotifier: SlackNotifier,
    private val config: Config,
) {

    data class Config(
        val cron: String,
        val maxRetries: Int,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Schedules.cron(config.cron))
        .execute { _, _ ->
            val retries = config.maxRetries
            val staleEvents: List<ArenaEvent> = arenaEventService.getStaleEvents(retriesGreaterThanOrEqual = retries)
            if (staleEvents.isNotEmpty()) {
                val message = """
                    Det finnes ${staleEvents.size} rader i tabellen 'arena_events' som har retries >= $retries og status Failed.
                """.trimIndent()
                slackNotifier.sendMessage(message)
            }
        }
}
