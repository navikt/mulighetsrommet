package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.TilgjengelighetsstatusSanitySyncService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class SynchronizeTilgjengelighetsstatuserToSanity(
    config: Config,
    tilgjengelighetsstatuserToSanity: TilgjengelighetsstatusSanitySyncService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val chronExpression: String,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tilgjengelighetsstatuser", Schedules.cron(config.chronExpression))
        .onFailure { failure, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tilgjengelighetsstatuser til Sanity. Konsekvensen er at tilgjengelighetsstatusene i Sanity vil være utdaterte. Cause: ${failure.cause.get().message}")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av tilgjengelighetsstatuser")
                tilgjengelighetsstatuserToSanity.synchronizeTilgjengelighetsstatus()
            }
        }
}
