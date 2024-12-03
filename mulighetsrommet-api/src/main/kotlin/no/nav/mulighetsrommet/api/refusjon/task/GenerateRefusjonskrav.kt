package no.nav.mulighetsrommet.api.refusjon.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.refusjon.RefusjonService
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
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

    @Serializable
    data class TaskInput(
        @Serializable(with = LocalDateSerializer::class)
        val dayInMonth: LocalDate,
    )

    val task: RecurringTask<TaskInput> = Tasks
        .recurring(javaClass.simpleName, config.toSchedule(), TaskInput::class.java)
        .execute { inst, _ ->
            if (inst.data == null) {
                runTask(LocalDate.now().minusMonths(1))
            } else {
                runTask(inst.data.dayInMonth)
            }
        }

    fun runTask(dayInMonth: LocalDate) {
        if (config.disabled) return
        refusjonService.genererRefusjonskravForMonth(dayInMonth)
    }
}
