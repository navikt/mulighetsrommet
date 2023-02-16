package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.KafkaSyncService
import no.nav.mulighetsrommet.api.services.Norg2Service
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SynchronizeStatusesOnKafka(config: Config, kafkaSyncService: KafkaSyncService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val delayOfMinutes: Int,
        val schedulerStatePollDelay: Long = 1000
    )

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-statuses-kafka", Daily(LocalTime.MIDNIGHT))
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av statuser på kafka")
                kafkaSyncService.oppdaterTiltaksgjennomforingsstatus(LocalDate.now(), LocalDate.ofInstant(context.execution.lastSuccess, ZoneId.systemDefault()))
            }
        }
}
