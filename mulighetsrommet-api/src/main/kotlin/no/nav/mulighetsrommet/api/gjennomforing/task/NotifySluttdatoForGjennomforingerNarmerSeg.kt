package no.nav.mulighetsrommet.api.gjennomforing.task

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.notifications.*
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate

class NotifySluttdatoForGjennomforingerNarmerSeg(
    config: Config,
    private val db: Database,
    private val notifications: NotificationRepository,
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
            notifySluttDatoNarmerSeg(today = LocalDate.now())
        }

    fun notifySluttDatoNarmerSeg(today: LocalDate) {
        val gjennomforinger = getAllGjennomforingerSomNarmerSegSluttdato(today)

        gjennomforinger.forEach { gjennomforing ->
            gjennomforing.administratorer.toNonEmptyListOrNull()?.also { administratorer ->
                val title = listOfNotNull(
                    "Gjennomføringen",
                    "\"${gjennomforing.navn}\"",
                    gjennomforing.tiltaksnummer?.let { "($it)" },
                    "utløper",
                    gjennomforing.sluttDato.formaterDatoTilEuropeiskDatoformat(),
                ).joinToString(" ")

                val notification = ScheduledNotification(
                    type = NotificationType.NOTIFICATION,
                    title = title,
                    targets = administratorer,
                    createdAt = Instant.now(),
                    metadata = NotificationMetadata(
                        linkText = "Gå til gjennomføringen",
                        link = "/tiltaksgjennomforinger/${gjennomforing.id}",
                    ),
                )

                notifications.insert(notification)
            }
        }
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(
        today: LocalDate,
    ): List<TiltaksgjennomforingNotificationDto> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id::uuid,
                   gjennomforing.navn,
                   gjennomforing.slutt_dato,
                   array_agg(distinct nav_ident) as administratorer,
                   gjennomforing.tiltaksnummer
            from tiltaksgjennomforing gjennomforing
                     join tiltaksgjennomforing_administrator on tiltaksgjennomforing_id = gjennomforing.id
            where (:today::timestamp + interval '14' day) = gjennomforing.slutt_dato
               or (:today::timestamp + interval '7' day) = gjennomforing.slutt_dato
               or (:today::timestamp + interval '1' day) = gjennomforing.slutt_dato
            group by gjennomforing.id
        """.trimIndent()

        queryOf(query, mapOf("today" to today))
            .map { it.toTiltaksgjennomforingNotificationDto() }
            .asList
            .runWithSession(session)
    }

    private fun Row.toTiltaksgjennomforingNotificationDto(): TiltaksgjennomforingNotificationDto {
        val administratorer = array<String>("administratorer").asList().map { NavIdent(it) }
        return TiltaksgjennomforingNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            sluttDato = localDate("slutt_dato"),
            administratorer = administratorer,
            tiltaksnummer = stringOrNull("tiltaksnummer"),
        )
    }
}
