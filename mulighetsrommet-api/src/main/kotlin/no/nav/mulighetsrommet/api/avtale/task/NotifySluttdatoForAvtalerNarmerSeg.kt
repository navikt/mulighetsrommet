package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.toNonEmptyListOrNull
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.model.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant

class NotifySluttdatoForAvtalerNarmerSeg(
    config: Config,
    notificationService: NotificationService,
    avtaleService: AvtaleService,
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
        .recurring(javaClass.simpleName, config.toSchedule())
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
