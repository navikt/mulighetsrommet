package no.nav.mulighetsrommet.api.tasks

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

class NotifySluttdatoForAvtalerNarmerSeg(
    config: Config,
    notificationService: NotificationService,
    avtaleService: AvtaleService,
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
        .recurring("notify-sluttdato-for-avtaler-narmer-seg", config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            slack.sendMessage(
                """
                Klarte ikke opprette notifikasjoner for avtaler som nærmer seg sluttdato.
                Konsekvensen er at brukere ikke nødvendigvis får med seg at sluttdato nærmer seg for sine avtaler.
                Detaljer: $cause
                """.trimIndent(),
            )
        }
        .execute { _, _ ->
            logger.info("Oppretter notifikasjoner for avtaler som nærmer seg sluttdato...")

            runBlocking {
                val avtaler: List<AvtaleNotificationDto> = avtaleService.getAllAvtalerSomNarmerSegSluttdato()

                avtaler.forEach {
                    it.administratorer.toNonEmptyListOrNull()?.let { administratorer ->
                        val notification = ScheduledNotification(
                            type = NotificationType.NOTIFICATION,
                            title = "Avtalen \"${it.navn}\" utløper ${
                                it.sluttDato?.formaterDatoTilEuropeiskDatoformat()
                            }",
                            targets = administratorer,
                            createdAt = Instant.now(),
                            metadata = NotificationMetadata(
                                linkText = "Gå til avtalen",
                                link = "/avtaler/${it.id}",
                            ),
                        )
                        notificationService.scheduleNotification(notification)
                    } ?: logger.info("Fant ingen administratorer for avtale med id: ${it.id}")
                }
            }
        }
}
