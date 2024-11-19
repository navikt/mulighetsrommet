package no.nav.mulighetsrommet.arena.adapter.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.database.Database
import java.time.Instant

class ReplayEvents(
    private val arenaEventService: ArenaEventService,
    val database: Database,
) {
    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .execute { _, _ ->
            runBlocking {
                arenaEventService.retryEvents(status = ArenaEvent.ProcessingStatus.Replay)
            }
        }

    private val client = SchedulerClient.Builder.create(database.getDatasource(), task).build()

    fun schedule(startTime: Instant = Instant.now()) {
        // Id er alltid det samme slik at bare en instans kan kjøre samtidig
        client.scheduleIfNotExists(task.instance("1"), startTime)
    }
}
