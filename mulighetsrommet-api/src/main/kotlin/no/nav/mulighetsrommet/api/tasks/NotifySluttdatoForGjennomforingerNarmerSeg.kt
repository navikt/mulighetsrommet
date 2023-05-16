package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

class NotifySluttdatoForGjennomforingerNarmerSeg(
    config: Config,
    notificationService: NotificationService,
    tiltaksgjennomforingService: TiltaksgjennomforingService,
    slack: SlackNotifier,
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
        .recurring("notify-sluttdato-for-tiltaksgjennomforinger-narmer-seg", config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            slack.sendMessage(
                """
                Klarte ikke opprette notifikasjoner for tiltaksgjennomføringer som nærmer seg sluttdato.
                Konsekvensen er at brukere ikke nødvendigvis får med seg at sluttdato nærmer seg for sine tiltaksgjennomføringer.
                Detaljer: $cause
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Oppretter notifikasjoner for tiltaksgjennomføringer som nærmer seg sluttdato...")

            runBlocking {
                val tiltaksgjennomforinger: List<TiltaksgjennomforingNotificationDto> =
                    tiltaksgjennomforingService.getAllGjennomforingerSomNarmerSegSluttdato()
                tiltaksgjennomforinger.forEach {
                    if (it.ansvarlige.isEmpty()) {
                        logger.info("Fant ingen ansvarlige for gjennomføring med id: ${it.id}")
                    } else {
                        val notification = ScheduledNotification(
                            type = NotificationType.Task,
                            title = "Sluttdato nærmer seg for tiltaksgjennomføring",
                            description = "Sluttdato nærmer seg for tiltaksgjennomføringen: ${it.navn}. Sluttdato er ${it.sluttDato}. Hvis gjennomføringen skal forlenges må du inn og endre til ny sluttdato. Hvis gjennomføringen ikke skal forlenges kan du se bort fra denne meldingen",
                            targets = it.ansvarlige,
                            createdAt = Instant.now(),
                        )
                        notificationService.scheduleNotification(notification)
                    }
                }
            }
        }
}
