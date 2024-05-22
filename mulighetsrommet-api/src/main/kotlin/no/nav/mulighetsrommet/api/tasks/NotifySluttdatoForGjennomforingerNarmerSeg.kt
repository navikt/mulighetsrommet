package no.nav.mulighetsrommet.api.tasks

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
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
                val europeanDatePattern = "dd.MM.yyyy"
                tiltaksgjennomforinger.forEach {
                    it.administratorer.toNonEmptyListOrNull()?.let { administratorer ->
                        val notification = ScheduledNotification(
                            type = NotificationType.NOTIFICATION,
                            title = "Gjennomføringen \"${it.navn} ${if (it.tiltaksnummer != null) "(${it.tiltaksnummer})" else ""}\" utløper ${
                                it.sluttDato?.format(
                                    DateTimeFormatter.ofPattern(europeanDatePattern),
                                )
                            }",
                            targets = administratorer,
                            createdAt = Instant.now(),
                            metadata = NotificationMetadata(
                                linkText = "Gå til gjennomføringen",
                                link = "/tiltaksgjennomforinger/${it.id}",
                            ),
                        )
                        notificationService.scheduleNotification(notification)
                    } ?: logger.info("Fant ingen administratorer for gjennomføring med id: ${it.id}")
                }
            }
        }
}
