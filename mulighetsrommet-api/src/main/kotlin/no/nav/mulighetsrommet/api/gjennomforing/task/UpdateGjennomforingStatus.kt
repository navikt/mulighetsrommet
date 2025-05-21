package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class UpdateGjennomforingStatus(
    private val db: ApiDatabase,
    private val gjennomforingService: GjennomforingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Daily(LocalTime.MIDNIGHT))
        .execute { _, _ ->
            oppdaterGjennomforingStatus(LocalDate.now())
        }

    fun oppdaterGjennomforingStatus(today: LocalDate) {
        logger.info("Oppdaterer status på gjennomføringer som skal avsluttes fra og med dato $today")

        val gjennomforinger = getGjennomforingerSomSkalAvsluttes(
            sluttDatoLessThan = today,
        )

        gjennomforinger.forEach { id ->
            logger.info("Avslutter gjennomføring id=$id")
            gjennomforingService.setAvsluttet(
                id = id,
                avsluttetTidspunkt = today.atStartOfDay(),
                avbruttAarsak = null,
                endretAv = Tiltaksadministrasjon,
            )
        }

        logger.info("Oppdaterte status for ${gjennomforinger.size} gjennomføringer")
    }

    private fun getGjennomforingerSomSkalAvsluttes(
        sluttDatoLessThan: LocalDate,
    ): List<UUID> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id
            from gjennomforing
            where status = 'GJENNOMFORES'
              and slutt_dato < :slutt_dato_lt
            order by created_at
        """.trimIndent()

        val params = mapOf(
            "slutt_dato_lt" to sluttDatoLessThan,
        )

        session.list(queryOf(query, params)) { it.uuid("id") }
    }
}
