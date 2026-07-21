package no.nav.mulighetsrommet.utdanning.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.admin.utdanning.SynkroniserUtdanningerCommand
import no.nav.mulighetsrommet.admin.utdanning.SynkroniserUtdanningerUseCase
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.client.toProgramomrade
import no.nav.mulighetsrommet.utdanning.client.toUtdanning
import org.slf4j.LoggerFactory

class SynchronizeUtdanninger(
    config: Config,
    private val utdanningClient: UtdanningClient,
    private val synkroniserUtdanninger: SynkroniserUtdanningerUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
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
            syncUtdanninger()
        }

    suspend fun syncUtdanninger() {
        val allUtdanninger = utdanningClient.getUtdanninger()

        val (programomradeRader, utdanningRader) = allUtdanninger.partition {
            it.sluttkompetanse == null && it.utdanningId == null && it.utdanningslop.size == 1
        }

        val command = SynkroniserUtdanningerCommand(
            programomrader = programomradeRader.map { it.toProgramomrade() },
            utdanninger = utdanningRader.filter { it.utdanningId != null }.map { it.toUtdanning() },
        )

        val skippedProgramomrader = synkroniserUtdanninger.execute(command)

        skippedProgramomrader.forEach { error ->
            logger.warn("Hopper over synkronisering av utdanningsprogram: $error")
        }
    }
}
