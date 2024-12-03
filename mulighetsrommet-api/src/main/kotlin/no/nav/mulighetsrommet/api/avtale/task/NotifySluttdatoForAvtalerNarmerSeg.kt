package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.notifications.*
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate

class NotifySluttdatoForAvtalerNarmerSeg(
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
            notifySluttDatoNarmerSeg(LocalDate.now())
        }

    fun notifySluttDatoNarmerSeg(today: LocalDate) {
        val avtaler = getAllAvtalerSomNarmerSegSluttdato(today)

        avtaler.forEach { avtale ->
            avtale.administratorer.toNonEmptyListOrNull()?.let { administratorer ->
                val title = listOfNotNull(
                    "Avtalen",
                    "\"${avtale.navn}\"",
                    "utløper",
                    avtale.sluttDato.formaterDatoTilEuropeiskDatoformat(),
                ).joinToString(" ")

                val notification = ScheduledNotification(
                    type = NotificationType.NOTIFICATION,
                    title = title,
                    targets = administratorer,
                    createdAt = Instant.now(),
                    metadata = NotificationMetadata(
                        linkText = "Gå til avtalen",
                        link = "/avtaler/${avtale.id}",
                    ),
                )

                notifications.insert(notification)
            }
        }
    }

    fun getAllAvtalerSomNarmerSegSluttdato(
        today: LocalDate,
    ): List<AvtaleNotificationDto> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select avtale.id::uuid,
                   avtale.navn,
                   avtale.slutt_dato,
                   array_agg(distinct nav_ident) as administratorer
            from avtale
                     join avtale_administrator on avtale.id = avtale_id
            where (:today::timestamp + interval '8' month) = avtale.slutt_dato
               or (:today::timestamp + interval '6' month) = avtale.slutt_dato
               or (:today::timestamp + interval '3' month) = avtale.slutt_dato
               or (:today::timestamp + interval '14' day) = avtale.slutt_dato
               or (:today::timestamp + interval '7' day) = avtale.slutt_dato
            group by avtale.id
        """.trimIndent()

        queryOf(query, mapOf("today" to today))
            .map { it.toAvtaleNotificationDto() }
            .asList
            .runWithSession(session)
    }

    private fun Row.toAvtaleNotificationDto(): AvtaleNotificationDto {
        val administratorer = array<String>("administratorer").asList().map { NavIdent(it) }
        return AvtaleNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            sluttDato = localDate("slutt_dato"),
            administratorer = administratorer,
        )
    }
}
