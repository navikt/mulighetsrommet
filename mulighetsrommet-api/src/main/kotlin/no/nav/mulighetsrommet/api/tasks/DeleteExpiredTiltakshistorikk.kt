package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.Tiltakshistorikk
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

class DeleteExpiredTiltakshistorikk(
    config: Config,
    private val tiltakshistorikk: TiltakshistorikkRepository,
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
        .recurring(javaClass.name, config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            val stackTrace = failure.cause.getOrNull()?.stackTraceToString()
            slack.sendMessage(
                """
                Klarte ikke slette utgått tiltakshistorikk. Konsekvensen er at databasen kan inneholde tiltakshistorikk
                lengre tilbake i tid enn det vi har skrevet i PVK.
                Detaljer: $cause
                Stacktrace: $stackTrace
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Sletter utgått tiltakshistorikk...")

            val expirationDate = LocalDate.now().minus(Tiltakshistorikk.TiltakshistorikkTimePeriod)
            tiltakshistorikk.deleteByExpirationDate(expirationDate)
                .onRight {
                    logger.info("Slettet $it rader med tiltakshistorikk")
                }
                .getOrThrow()
        }
}
