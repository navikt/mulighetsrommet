package no.nav.mulighetsrommet.api.navenhet.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.navenhet.NavEnheterSyncService
import org.slf4j.LoggerFactory

class SynchronizeNorgEnheter(
    config: Config,
    navEnheterSyncService: NavEnheterSyncService,
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
        .recurring(javaClass.simpleName, config.toSchedule())
        .execute { _, _ ->
            runBlocking {
                logger.info("Kjører synkronisering av NORG2-enheter")
                navEnheterSyncService.synkroniserEnheter()
            }
        }
}
