package no.nav.mulighetsrommet.api.navenhet.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.navenhet.NavEnheterSyncService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class SynchronizeNorgEnheter(
    config: Config,
    navEnheterSyncService: NavEnheterSyncService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val disabled: Boolean = false,
    ) {
        fun toSchedule(): Schedule {
            return if (disabled) {
                DisabledSchedule()
            } else {
                FixedDelay.ofMinutes(delayOfMinutes)
            }
        }
    }

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-norg2-enheter", config.toSchedule())
        .onFailure { failure, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere enheter fra NORG2. Konsekvensen er at ingen enheter blir oppdaterte i databasen og vi kan potensielt vise feil enheter til bruker i admin-flate. Cause: ${failure.cause.get().message}")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av NORG2-enheter")
                navEnheterSyncService.synkroniserEnheter()
            }
        }
}
