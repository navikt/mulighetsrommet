package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.Norg2Service
import org.slf4j.LoggerFactory

class SynchronizeNorgEnheter(config: Config, norg2Service: Norg2Service) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val schedulerStatePollDelay: Long = 1000
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-norg2-enheter", FixedDelay.ofMinutes(config.delayOfMinutes))
        .execute { instance, context ->
            runBlocking {
                async {
                    norg2Service.synkroniserEnheter()
                }
            }
        }
}
