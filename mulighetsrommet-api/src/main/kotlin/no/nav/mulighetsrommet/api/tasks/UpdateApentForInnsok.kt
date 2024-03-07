package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

class UpdateApentForInnsok(
    config: Config,
    database: Database,
    tiltaksgjennomforingService: TiltaksgjennomforingService,
    endringshistorikkService: EndringshistorikkService,
    slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
    ) {
        fun toSchedule(): Schedule {
            return if (disabled) {
                DisabledSchedule()
            } else {
                Schedules.cron(cronPattern)
            }
        }
    }

    val task: RecurringTask<Void> = Tasks
        .recurring("update-apent-for-innsok", config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            slack.sendMessage(
                """
                Klarte ikke oppdatere Åpent for innsøk for tiltak der sluttdato har passert.
                Konsekvensen er at tiltak kan stå som at de er åpne for innsøk når de egentlig ikke er det og redaktører må manuelt rydde opp.
                Detaljer: $cause
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Oppdaterer Åpent for innsøk for tiltak med startdato i dag...")

            runBlocking {
                database.transaction { tx ->
                    val antallTiltak =
                        tiltaksgjennomforingService.batchApentForInnsokForAlleMedStarttdatoForDato(LocalDate.now(), tx)
                    logger.info("Oppdaterte $antallTiltak tiltak med åpent for innsøk = false")
                }
            }
        }
}
