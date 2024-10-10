package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.okonomi.refusjon.RefusjonService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate

class GenerateRefusjonskrav(
    private val config: Config,
    private val refusjonService: RefusjonService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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
        .recurring("generate-refusjonskrav", config.toSchedule())
        .onFailure { failure, _ ->
            slackNotifier.sendMessage("Klarte ikke generere refusjonskrav. Cause: ${failure.cause.get().message}")
        }
        .execute { _, _ ->
            logger.info("Genererer refusjonskrav")

            val dayInPreviousMonth = LocalDate.now().minusMonths(1)
            runTask(dayInPreviousMonth)
        }

    fun runTask(dayInMonth: LocalDate) {
        if (config.disabled) return
        refusjonService.genererRefusjonskravForMonth(dayInMonth)
    }
}
