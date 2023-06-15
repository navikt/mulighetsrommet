package no.nav.mulighetsrommet.api.tasks

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
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class Group(
    val adGruppe: UUID,
    val rolle: NavAnsattRolle,
)

class SynchronizeNavAnsatte(
    config: Config,
    msGraphClient: MicrosoftGraphClient,
    ansatte: NavAnsattRepository,
    slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
        val groups: List<Group> = listOf(),
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
                config.groups.forEach { group ->
                    logger.info("Synkroniserer brukere i AD-gruppe id=${group.adGruppe}")

                    val members = msGraphClient.getGroupMembers(group.adGruppe)
                    members.forEach { ansatt ->
                        val dbo = ansatt.run {
                            NavAnsattDbo(
                                navIdent = navident,
                                fornavn = fornavn,
                                etternavn = etternavn,
                                hovedenhet = hovedenhetKode,
                                azureId = azureId,
                                fraAdGruppe = group.adGruppe,
                                mobilnummer = mobilnr,
                                epost = epost,
                                rolle = group.rolle,
                            )
                        }

                        ansatte.upsert(dbo).onLeft {
                            throw it.error
                        }
                    }
                }
            }
        }
}
