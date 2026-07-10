package no.nav.mulighetsrommet.api.navenhet.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import no.nav.mulighetsrommet.admin.navenhet.SynkroniserNavEnheterCommand
import no.nav.mulighetsrommet.admin.navenhet.SynkroniserNavEnheterUseCase
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.toNavEnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.toNavEnhetType
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.navenhet.service.SanityNavEnhetPublisher
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.slf4j.LoggerFactory

class SynchronizeNorgEnheter(
    config: Config,
    private val norg2Client: Norg2Client,
    private val synkroniserNavEnheter: SynkroniserNavEnheterUseCase,
    private val sanityNavEnhetPublisher: SanityNavEnhetPublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int = 360,
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
        .recurring(javaClass.simpleName, config.toSchedule())
        .executeSuspend { _, _ ->
            synkroniserEnheter()
        }

    suspend fun synkroniserEnheter() {
        val enheter = norg2Client.hentEnheter()

        logger.info("Hentet ${enheter.size} enheter fra NORG2")

        val navEnheter = enheter.map { (enhet, overordnetEnhet) ->
            NavEnhet(
                navn = enhet.navn,
                enhetsnummer = enhet.enhetNr,
                status = enhet.status.toNavEnhetStatus(),
                type = enhet.type.toNavEnhetType(),
                overordnetEnhet = overordnetEnhet,
            )
        }

        val synkroniserteEnheter = synkroniserNavEnheter.execute(SynkroniserNavEnheterCommand(navEnheter))

        sanityNavEnhetPublisher.publish(synkroniserteEnheter)
    }
}
