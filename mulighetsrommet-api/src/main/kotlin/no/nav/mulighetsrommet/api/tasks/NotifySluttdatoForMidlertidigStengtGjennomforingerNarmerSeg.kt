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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.jvm.optionals.getOrNull

class NotifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg(
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
        .recurring("notify-sluttdato-for-midlertidig-stengt-tiltaksgjennomforinger-narmer-seg", config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            slack.sendMessage(
                """
                Klarte ikke opprette notifikasjoner for midlertidig stengte tiltaksgjennomføringer som nærmer seg sluttdato for den stengte perioden.
                Konsekvensen er at brukere ikke nødvendigvis får med seg at sluttdato nærmer seg for sine midlertidig stengte tiltaksgjennomføringer.
                Detaljer: $cause
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Oppretter notifikasjoner for midlertidig stengte tiltaksgjennomføringer som nærmer seg sluttdato...")

            runBlocking {
                val tiltaksgjennomforinger: List<TiltaksgjennomforingNotificationDto> =
                    tiltaksgjennomforingService.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
                tiltaksgjennomforinger.forEach {
                    if (it.ansvarlige.isEmpty()) {
                        logger.info("Fant ingen ansvarlige for gjennomføring med id: ${it.id}")
                    } else {
                        val notification = ScheduledNotification(
                            type = NotificationType.NOTIFICATION,
                            title = "Midlertidig stengt periode for ${FrontendRoutes.lenkeTilGjennomforing("gjennomføring \"${it.navn}\"", it.id)} ${if (it.tiltaksnummer != null) "(${it.tiltaksnummer})" else ""}\" utløper ${
                                it.stengtTil?.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
                                )
                            }.",
                            targets = it.ansvarlige,
                            createdAt = Instant.now(),
                        )
                        notificationService.scheduleNotification(notification)
                    }
                }
            }
        }
}
