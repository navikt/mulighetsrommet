package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService

class RetryFailedEvents(
    config: Config,
    private val arenaEventService: ArenaEventService,
) {

    data class Config(
        val delayOfMinutes: Int,
        val schedulerStatePollDelay: Long = 1000,
    )

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, FixedDelay.ofMinutes(config.delayOfMinutes))
        .execute { _, _ ->
            runBlocking {
                arenaEventService.retryEvents(status = ArenaEvent.ProcessingStatus.Failed)
            }
        }
}
