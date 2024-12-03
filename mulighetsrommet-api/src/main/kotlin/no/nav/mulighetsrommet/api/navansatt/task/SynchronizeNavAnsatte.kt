package no.nav.mulighetsrommet.api.navansatt.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.navansatt.NavAnsattSyncService
import no.nav.mulighetsrommet.tasks.executeSuspend
import java.time.LocalDate
import java.time.Period

class SynchronizeNavAnsatte(
    config: Config,
    private val navAnsattSyncService: NavAnsattSyncService,
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
        .executeSuspend { _, _ ->
            val today = LocalDate.now()
            val deletionDate = today.plus(config.deleteNavAnsattGracePeriod)
            navAnsattSyncService.synchronizeNavAnsatte(today, deletionDate)
        }
}
