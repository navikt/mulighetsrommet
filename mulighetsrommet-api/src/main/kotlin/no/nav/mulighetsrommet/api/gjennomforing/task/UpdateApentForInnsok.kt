package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.tasks.RecurringTaskWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class UpdateApentForInnsok(
    config: Config,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
) : RecurringTaskWrapper<Void>(config.toSchedule()) {
    override val log: Logger = LoggerFactory.getLogger(javaClass)

    override val descriptor: TaskDescriptor<Void> = TaskDescriptor.of(javaClass.name)

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

//    val task: RecurringTask<Void> = Tasks
//        .recurring("update-apent-for-innsok", config.toSchedule())
//        .onFailure { failure, _ ->
//            val cause = failure.cause.getOrNull()?.message
//            slack.sendMessage(
//                """
//                Klarte ikke oppdatere Åpent for innsøk for tiltak der startdato har passert.
//                Konsekvensen er at tiltak kan stå at de er åpne for innsøk når de egentlig ikke er det og redaktører må manuelt rydde opp.
//                Detaljer: $cause
//                """.trimIndent(),
//            )
//        }
//        .execute { _, _ ->
//
//            runBlocking {
//                tiltaksgjennomforingService.batchApentForInnsokForAlleMedStarttdatoForDato(LocalDate.now())
//            }
//        }

    override suspend fun execute(instance: TaskInstance<Void>, context: ExecutionContext) {
        log.info("Oppdaterer Åpent for innsøk for tiltak med startdato i dag...")
        tiltaksgjennomforingService.batchApentForInnsokForAlleMedStarttdatoForDato(LocalDate.now())
    }
}
