package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.SanityTiltaksgjennomforingEnheterTilApiService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class SynchronizeTiltaksgjennomforingEnheter(
    config: Config,
    sanityTiltaksgjennomforingEnheterTilApiService: SanityTiltaksgjennomforingEnheterTilApiService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val disabled: Boolean = false,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tiltaksgjennomforing-enheter", FixedDelay.ofMinutes(config.delayOfMinutes))
        .onFailure { _, _ ->
            logger.error("Klarte ikke synkronisere tiltaksgjennomføringenheter fra Sanity til database")
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomføringenheter fra Sanity til database.")
        }
        .execute { _, _ ->
            runBlocking {
                if (config.disabled) return@runBlocking

                logger.info("Kjører synkronisering av tiltaksgjennomforingsenheter fra Sanity")
                sanityTiltaksgjennomforingEnheterTilApiService.oppdaterTiltaksgjennomforingEnheter()
            }
        }
}
