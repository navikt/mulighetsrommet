package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.KafkaSyncService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SynchronizeStatusesOnKafka(kafkaSyncService: KafkaSyncService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        // .recurring("synchronize-statuses-kafka", Daily(LocalTime.MIDNIGHT))
        .recurring("synchronize-statuses-kafka", Daily(LocalTime.now().plusMinutes(5)))
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av statuser på kafka")
                kafkaSyncService.oppdaterTiltaksgjennomforingsstatus(LocalDate.now(), context.execution.lastSuccess?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) } ?: LocalDate.of(2023, 2, 1))
            }
        }
}
