package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import io.ktor.server.plugins.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.*
import kotlin.jvm.optionals.getOrNull

class SynchronizeNavAnsatte(
    config: Config,
    private val navAnsattService: NavAnsattService,
    slack: SlackNotifier,
    database: Database,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
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
            val stackTrace = failure.cause.getOrNull()?.stackTraceToString()
            slack.sendMessage(
                """
                Klarte ikke synkronisere NAV-ansatte fra Azure AD.
                Konsekvensen er at databasen over NAV-ansatte i løsningen kan være utdatert.
                Detaljer: $cause
                Stacktrace: $stackTrace
                """.trimIndent(),
            )
        }
        .execute { instance, _ ->
            MDC.put("correlationId", instance.id)

            logger.info("Synkroniserer NAV-ansatte fra Azure til database...")

            runBlocking {
                val today = LocalDate.now()
                val deletionDate = today.plus(config.deleteNavAnsattGracePeriod)
                navAnsattService.synchronizeNavAnsatte(today, deletionDate)
            }
        }

    private val client = SchedulerClient.Builder.create(database.getDatasource(), task).build()
    fun schedule(startTime: Instant = Instant.now()): UUID {
        val existingTaskId = task.defaultTaskInstance.id
        val existingSchedule = client.getScheduledExecution(task.instance(existingTaskId)).get()

        if (existingSchedule.isPicked) {
            throw BadRequestException("Synkronisering av ansatte kjører allerede.")
        }

        client.reschedule(task.instance(existingTaskId), startTime.plusSeconds(30))
        return UUID.randomUUID()
    }
}
