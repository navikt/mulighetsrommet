package no.nav.mulighetsrommet.api.tasks

import arrow.core.Either
import arrow.core.continuations.either
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.services.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import kotlin.jvm.optionals.getOrNull

class SynchronizeNavAnsatte(
    config: Config,
    private val navAnsattService: NavAnsattService,
    private val ansatte: NavAnsattRepository,
    slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
        val groups: List<AdGruppeNavAnsattRolleMapping> = emptyList(),
        val deleteNavAnsattGracePeriod: Period = Period.ofDays(30),
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
        .recurring("synchronize-nav-ansatte", config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            val stackTrace = failure.cause.getOrNull()?.stackTrace
            slack.sendMessage(
                """
                Klarte ikke synkronisere NAV-ansatte fra AD-grupper med id=${config.groups}.
                Konsekvensen er at databasen over NAV-ansatte i løsningen kan være utdatert.
                Detaljer: $cause
                Stacktrace: $stackTrace
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Synkroniserer NAV-ansatte fra Azure til database...")

            runBlocking {
                val today = LocalDate.now()
                val deletionDate = today.plus(config.deleteNavAnsattGracePeriod)
                synchronizeNavAnsatte(config.groups, today, deletionDate)
            }
        }

    internal suspend fun synchronizeNavAnsatte(
        roles: List<AdGruppeNavAnsattRolleMapping>,
        today: LocalDate,
        navAnsattDeletionDate: LocalDate,
    ): Either<DatabaseOperationError, Unit> = either {
        val ansatteToUpsert = navAnsattService.getNavAnsatteWithRoles(roles)
        ansatteToUpsert.forEach { ansatt ->
            ansatte.upsert(ansatt).bind()
        }

        val ansatteToScheduleForDeletion = ansatte.getAll()
            .map { it.filter { ansatt -> ansatt !in ansatteToUpsert && ansatt.skalSlettesDato == null } }
            .bind()
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting azureId=${ansatt.azureId} dato=$navAnsattDeletionDate")
            ansatte.upsert(ansatt.copy(roller = emptyList(), skalSlettesDato = navAnsattDeletionDate)).bind()
        }

        val ansatteToDelete = ansatte.getAll(skalSlettesDatoLte = today).bind()
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting azureId=${ansatt.azureId} dato=${ansatt.skalSlettesDato}")
            ansatte.deleteByAzureId(ansatt.azureId).bind()
        }
    }
}
