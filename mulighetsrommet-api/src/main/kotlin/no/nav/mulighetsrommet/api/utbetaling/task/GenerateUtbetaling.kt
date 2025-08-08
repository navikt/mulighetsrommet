package no.nav.mulighetsrommet.api.utbetaling.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.tasks.executeSuspend
import java.time.LocalDate

class GenerateUtbetaling(
    private val config: Config,
    private val utbetalinger: GenererUtbetalingService,
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
            val periode = Periode.forMonthOf(LocalDate.now().minusMonths(1))
            runTask(periode)
        }

    suspend fun runTask(periode: Periode): List<Utbetaling> {
        if (config.disabled) {
            return listOf()
        }

        return utbetalinger.genererUtbetalingForPeriode(periode)
    }
}
