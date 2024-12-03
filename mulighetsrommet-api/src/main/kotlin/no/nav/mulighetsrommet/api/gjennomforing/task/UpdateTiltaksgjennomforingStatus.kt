package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class UpdateTiltaksgjennomforingStatus(
    private val db: Database,
    private val gjennomforingService: TiltaksgjennomforingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Daily(LocalTime.MIDNIGHT))
        .execute { _, _ ->
            oppdaterTiltaksgjennomforingStatus(LocalDate.now())
        }

    fun oppdaterTiltaksgjennomforingStatus(today: LocalDate) {
        logger.info("Oppdaterer status på gjennomføringer som skal avsluttes fra og med dato $today")

        val gjennomforinger = getGjennomforingerSomSkalAvsluttes(
            sluttDatoLessThan = today,
        )

        gjennomforinger.forEach { id ->
            logger.info("Avslutter gjennomføring id=$id")
            gjennomforingService.setAvsluttet(
                id = id,
                avsluttetTidspunkt = today.atStartOfDay(),
                avsluttetAarsak = null,
                endretAv = EndretAv.System,
            )
        }

        logger.info("Oppdaterte status for ${gjennomforinger.size} gjennomføringer")
    }

    private fun getGjennomforingerSomSkalAvsluttes(
        sluttDatoLessThan: LocalDate,
    ): List<UUID> = db.useSession { tx ->
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id,
                   gjennomforing.slutt_dato,
                   tiltaksgjennomforing_status(gjennomforing.start_dato, gjennomforing.slutt_dato, gjennomforing.avsluttet_tidspunkt) as current_status
            from tiltaksgjennomforing gjennomforing
                     join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
            where gjennomforing.avsluttet_tidspunkt is null
              and gjennomforing.slutt_dato < :slutt_dato_lt
            order by gjennomforing.id
            limit :limit offset :offset
        """.trimIndent()

        val params = mapOf(
            "slutt_dato_lt" to sluttDatoLessThan,
        )

        queryOf(query, params)
            .map { it.uuid("id") }
            .asList
            .runWithSession(tx)
    }
}
