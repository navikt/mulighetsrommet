package no.nav.mulighetsrommet.api.gjennomforing.task

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant

class NotifySluttdatoForGjennomforingerNarmerSeg(
    config: Config,
    notificationService: NotificationService,
    tiltaksgjennomforingService: TiltaksgjennomforingService,
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
            val gjennomforinger = tiltaksgjennomforingService.getAllGjennomforingerSomNarmerSegSluttdato()

            gjennomforinger.forEach {
                it.administratorer.toNonEmptyListOrNull()?.let { administratorer ->
                    val notification = ScheduledNotification(
                        type = NotificationType.NOTIFICATION,
                        title = "Gjennomføringen \"${it.navn} ${if (it.tiltaksnummer != null) "(${it.tiltaksnummer})" else ""}\" utløper ${
                            it.sluttDato?.formaterDatoTilEuropeiskDatoformat()
                        }",
                        targets = administratorer,
                        createdAt = Instant.now(),
                        metadata = NotificationMetadata(
                            linkText = "Gå til gjennomføringen",
                            link = "/tiltaksgjennomforinger/${it.id}",
                        ),
                    )
                    notificationService.scheduleNotification(notification)
                }
            }
        }
}
