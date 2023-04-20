package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.KafkaSyncService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SynchronizeTiltakstypestatuserToKafka(
    kafkaSyncService: KafkaSyncService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tiltakstypestatuser-kafka", Daily(LocalTime.MIDNIGHT))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltakstypestatuser på kafka. Konsekvensen er at statuser på tiltakstyper kan være utdaterte på kafka.")
        }
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltakstypestatuser på kafka")
                kafkaSyncService.oppdaterTiltakstypestatus(
                    LocalDate.now(),
                    context.execution.lastSuccess?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
                        ?: LocalDate.of(2023, 2, 1),
                )
            }
        }
}
