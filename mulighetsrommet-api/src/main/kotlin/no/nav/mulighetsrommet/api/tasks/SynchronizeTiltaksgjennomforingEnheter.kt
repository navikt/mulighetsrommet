package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
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
        .recurring("synchronize-tiltaksgjennomforing-enheter", config.toSchedule())
        .onFailure { failure, _ ->
            logger.error("Klarte ikke synkronisere tiltaksgjennomføringenheter fra Sanity til database: ${failure.cause}", failure)
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomføringenheter fra Sanity til database. Årsak: ${failure.cause}")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltaksgjennomforingsenheter fra Sanity")
                sanityTiltaksgjennomforingEnheterTilApiService.oppdaterTiltaksgjennomforingEnheter()
            }
        }
}
