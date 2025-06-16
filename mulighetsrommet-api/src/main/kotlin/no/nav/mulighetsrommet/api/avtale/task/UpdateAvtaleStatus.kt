package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class UpdateAvtaleStatus(
    private val db: ApiDatabase,
    private val service: AvtaleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Daily(LocalTime.MIDNIGHT))
        .execute { _, _ ->
            execute(LocalDateTime.now())
        }

    fun execute(now: LocalDateTime) {
        logger.info("Oppdaterer status på avtaler som skal avsluttes fra og med dato $now")

        val avtaler = getAvtalerSomSkalAvsluttes(
            sluttDatoLessThan = now.toLocalDate(),
        )

        avtaler.forEach { id ->
            logger.info("Avslutter avtale id=$id")
            service.avsluttAvtale(
                id = id,
                avsluttetTidspunkt = now,
                endretAv = Tiltaksadministrasjon,
            ).getOrElse { throw IllegalStateException(it.detail) }
        }

        logger.info("Oppdaterte status for ${avtaler.size} avtaler")
    }

    private fun getAvtalerSomSkalAvsluttes(
        sluttDatoLessThan: LocalDate,
    ): List<UUID> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id
            from avtale
            where status = 'AKTIV'
              and slutt_dato < :slutt_dato_lt
            order by created_at
        """.trimIndent()

        val params = mapOf(
            "slutt_dato_lt" to sluttDatoLessThan,
        )

        session.list(queryOf(query, params)) { it.uuid("id") }
    }
}
