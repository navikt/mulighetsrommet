package no.nav.mulighetsrommet.api.utbetaling.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.tasks.executeSuspend
import java.time.LocalDate

class GenerateUtbetaling(
    private val config: Config,
    private val utbetalingService: UtbetalingService,
) {
    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String?,
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
            val dayInPreviousMonth = LocalDate.now().minusMonths(1)
            runTask(dayInPreviousMonth)
        }

    suspend fun runTask(dayInMonth: LocalDate) {
        if (config.disabled) return
        utbetalingService.genererUtbetalingForMonth(dayInMonth)
    }
}
