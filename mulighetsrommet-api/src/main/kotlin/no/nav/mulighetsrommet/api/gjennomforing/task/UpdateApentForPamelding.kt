package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import java.time.LocalDate

class UpdateApentForPamelding(
    config: Config = Config(),
    private val db: Database,
    private val tiltaksgjennomforingService: TiltaksgjennomforingService,
) {
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
        .execute { _, _ ->
            stengTiltakMedFellesOppstartForPamelding(LocalDate.now())
        }

    fun stengTiltakMedFellesOppstartForPamelding(startDato: LocalDate) = db.transaction { tx ->
        @Language("PostgreSQL")
        val query = """
                select id
                from tiltaksgjennomforing
                where apent_for_pamelding = true
                  and oppstart = 'FELLES'
                  and start_dato = ?
        """.trimIndent()

        val ids = queryOf(query, startDato)
            .map { it.uuid("id") }
            .asList
            .runWithSession(tx)

        ids.forEach { id ->
            tiltaksgjennomforingService.setApentForPamelding(id, apentForPamelding = false, EndretAv.System)
        }
    }
}
