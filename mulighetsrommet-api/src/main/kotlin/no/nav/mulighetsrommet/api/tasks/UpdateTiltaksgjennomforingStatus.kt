package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class UpdateTiltaksgjennomforingStatus(
    slackNotifier: SlackNotifier,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.name, Daily(LocalTime.MIDNIGHT))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltaksgjennomføringsstatuser på kafka. Konsekvensen er at statuser på tiltaksgjennomføringer kan være utdaterte på kafka.")
        }
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltaksgjennomforingsstatuser på kafka")

                val lastSuccessDate = context.execution.lastSuccess
                    ?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
                    ?: LocalDate.of(2023, 2, 1)

                oppdaterTiltaksgjennomforingStatus(LocalDate.now(), lastSuccessDate)
            }
        }

    fun oppdaterTiltaksgjennomforingStatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for gjennomføringer med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = DatabaseUtils.paginate(limit = 1000) { paginationParams ->
            val tiltaksgjennomforinger = tiltaksgjennomforingRepository.getAllByDateIntervalAndNotAvbrutt(
                dateIntervalStart = lastSuccessDate,
                dateIntervalEnd = today,
                pagination = paginationParams,
            )

            tiltaksgjennomforinger.forEach { id ->
                val gjennomforing = requireNotNull(tiltaksgjennomforingRepository.get(id))
                tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(gjennomforing))
            }

            tiltaksgjennomforinger
        }
        logger.info("Oppdaterte status for $numberOfUpdates tiltaksgjennomføringer")
    }
}
