package no.nav.mulighetsrommet.api.gjennomforing.task

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingNotificationDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationTask
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate

class NotifySluttdatoForGjennomforingerNarmerSeg(
    config: Config,
    private val db: ApiDatabase,
    private val notificationTask: NotificationTask,
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
                    title = title,
                    targets = administratorer.map { it.navIdent },
                    createdAt = Instant.now(),
                    metadata = NotificationMetadata(
                        linkText = "Gå til gjennomføringen",
                        link = "/gjennomforinger/${gjennomforing.id}",
                    ),
                )

                notificationTask.scheduleNotification(notification)
            }
        }
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(
        today: LocalDate,
    ): List<GjennomforingNotificationDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id::uuid,
                   arena_tiltaksnummer,
                   navn,
                   slutt_dato,
                   administratorer_json
            from view_gjennomforing
            where gjennomforing_type = 'AVTALE' and (
               (:today::timestamp + interval '14' day) = slutt_dato
               or (:today::timestamp + interval '7' day) = slutt_dato
               or (:today::timestamp + interval '1' day) = slutt_dato)
        """.trimIndent()

        session.list(queryOf(query, mapOf("today" to today))) { it.toTiltaksgjennomforingNotificationDto() }
    }
}

private fun Row.toTiltaksgjennomforingNotificationDto(): GjennomforingNotificationDto {
    val administratorer = stringOrNull("administratorer_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtaleDetaljer.Administrator>>(it) }
        ?: emptyList()
    return GjennomforingNotificationDto(
        id = uuid("id"),
        navn = string("navn"),
        sluttDato = localDate("slutt_dato"),
        administratorer = administratorer,
        tiltaksnummer = stringOrNull("arena_tiltaksnummer"),
    )
}
