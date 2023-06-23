package no.nav.mulighetsrommet.api.tasks

import arrow.core.Either
import arrow.core.continuations.either
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class Group(
    val adGruppe: UUID,
    val rolle: NavAnsattRolle,
)

class SynchronizeNavAnsatte(
    config: Config,
    private val msGraphClient: MicrosoftGraphClient,
    private val ansatte: NavAnsattRepository,
    slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
        val groups: List<Group> = listOf(),
        val navAnsattDeleteGracePeriod: Duration = Duration.ofDays(30),
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
            slack.sendMessage(
                """
                Klarte ikke synkronisere NAV-ansatte fra AD-grupper med id=${config.groups}.
                Konsekvensen er at databasen over NAV-ansatte i løsningen kan være utdatert.
                Detaljer: $cause
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Synkroniserer NAV-ansatte fra Azure til database...")

            runBlocking {
                val deletionDate = LocalDate.now().plus(config.navAnsattDeleteGracePeriod)
                synchronizeNavAnsatte(config.groups, deletionDate)
            }
        }

    internal suspend fun synchronizeNavAnsatte(
        groups: List<Group>,
        navAnsattDeletionDate: LocalDate?,
    ): Either<DatabaseOperationError, Unit> = either {
        val ansatteToUpsert = resolveNavAnsatte(groups, msGraphClient)
        ansatteToUpsert.forEach { ansatt ->
            ansatte.upsert(ansatt).bind()
        }

        val ansatteToScheduleForDeletion = ansatte.getAll()
            .map { it.filter { ansatt -> ansatt !in ansatteToUpsert && ansatt.skalSlettesDato == null } }
            .bind()
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting azureId=${ansatt.azureId} dato=$navAnsattDeletionDate")
            ansatte.upsert(ansatt.copy(skalSlettesDato = navAnsattDeletionDate)).bind()
        }

        val ansatteToDelete = ansatte.getAll(skalSlettesDatoLte = LocalDate.now()).bind()
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi fordi vi har passert dato for sletting azureId=${ansatt.azureId} dato=${ansatt.skalSlettesDato}")
            ansatte.deleteByAzureId(ansatt.azureId).bind()
        }
    }
}

internal suspend fun resolveNavAnsatte(groups: List<Group>, microsoftGraphClient: MicrosoftGraphClient) = groups
    .flatMap { group ->
        val members = microsoftGraphClient.getGroupMembers(group.adGruppe)
        members.map { ansatt ->
            NavAnsattDbo.fromDto(ansatt, listOf(group.rolle))
        }
    }
    .groupBy { it.navIdent }
    .map { (_, value) ->
        value.reduce { a1, a2 ->
            a1.copy(roller = a1.roller + a2.roller)
        }
    }
