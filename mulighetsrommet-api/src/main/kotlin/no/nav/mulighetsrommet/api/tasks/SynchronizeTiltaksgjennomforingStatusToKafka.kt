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

class SynchronizeTiltaksgjennomforingStatusToKafka(
    kafkaSyncService: KafkaSyncService,
    slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring("synchronize-tiltaksgjennomforingsstatuser-kafka", Daily(LocalTime.MIDNIGHT))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomføringsstatuser på kafka. Konsekvensen er at statuser på tiltaksgjennomføringer kan være utdaterte på kafka.")
        }
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltaksgjennomforingsstatuser på kafka")
                kafkaSyncService.oppdaterTiltaksgjennomforingStatus(
                    LocalDate.now(),
                    context.execution.lastSuccess?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
                        ?: LocalDate.of(2023, 2, 1),
                )
            }
        }
}
