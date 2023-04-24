package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.SanityService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory


class SynchronizeTiltaksgjennomforingEnheter(
    sanityService: SanityService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tiltaksgjennomforing-enheter", FixedDelay.ofHours(1))
        .onFailure { _, _ ->
            logger.error("av tiltaksgjennomforingsstatuser på kafka")
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomføringenheter.")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltaksgjennomforingsenheter fra Sanity")
                sanityService.oppdaterTiltaksgjennomforingEnheter()
            }
        }
}
