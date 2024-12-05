package no.nav.mulighetsrommet.api.refusjon.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.refusjon.RefusjonService
import java.time.LocalDate

class GenerateRefusjonskrav(
    private val config: Config,
    private val refusjonService: RefusjonService,
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
        .execute { _, _ ->
            runTask(LocalDate.now().minusMonths(1))
        }

    fun runTask(dayInMonth: LocalDate) {
        if (config.disabled) return
        refusjonService.genererRefusjonskravForMonth(dayInMonth)
    }
}
