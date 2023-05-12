package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.SanityTiltaksgjennomforingService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class SynchronizeTiltaksgjennomforingerToSanity(
    config: Config,
    sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val disabled: Boolean = false,
        val dryRun: Boolean = false,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tiltaksgjennomforinger-to-sanity", FixedDelay.ofMinutes(config.delayOfMinutes))
        .onFailure { failure, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomforinger til Sanity. Konsekvensen er at tiltaksgjennomforinger ikke vil ha dokument i Sanity. Cause: ${failure.cause.get().message}")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltaksgjennomforinger")
                sanityTiltaksgjennomforingService.syncTiltaksgjennomforingerTilSanity(config.dryRun)
            }
        }
}
