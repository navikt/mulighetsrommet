package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.MetrikkService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class OppdaterMetrikker(
    config: Config,
    metrikkService: MetrikkService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val disabled: Boolean = false,
    ) {
        fun toSchedule(): Schedule {
            return if (disabled) {
                DisabledSchedule()
            } else {
                FixedDelay.ofMinutes(delayOfMinutes)
            }
        }
    }

    val task: RecurringTask<Void> = Tasks
        .recurring("oppdater-metrikker", config.toSchedule())
        .onFailure { failure, _ ->
            slackNotifier.sendMessage("Klarte ikke oppdatere metrikker til Prometheus. Cause: ${failure.cause.get().message}")
        }
        .execute { _, _ ->
            runBlocking {
                logger.info("Oppdaterer metrikker fra database til Prometheus")
                metrikkService.oppdaterMetrikker()
            }
        }
}
