package no.nav.mulighetsrommet.api.navansatt.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import io.ktor.server.plugins.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.navansatt.NavAnsattSyncService
import no.nav.mulighetsrommet.database.Database
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.*

class SynchronizeNavAnsatte(
    config: Config,
    private val navAnsattSyncService: NavAnsattSyncService,
    database: Database,
) {
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
        .recurring(javaClass.simpleName, config.toSchedule())
        .execute { _, _ ->
            runBlocking {
                val today = LocalDate.now()
                val deletionDate = today.plus(config.deleteNavAnsattGracePeriod)
                navAnsattSyncService.synchronizeNavAnsatte(today, deletionDate)
            }
        }

    private val client = SchedulerClient.Builder.create(database.getDatasource(), task).build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val existingTaskId = task.defaultTaskInstance.id
        val existingSchedule = client.getScheduledExecution(task.instance(existingTaskId)).get()

        if (existingSchedule.isPicked) {
            throw BadRequestException("Synkronisering av ansatte kjører allerede.")
        }

        client.reschedule(task.instance(existingTaskId), startTime.plusSeconds(5))
        return UUID.randomUUID()
    }
}
