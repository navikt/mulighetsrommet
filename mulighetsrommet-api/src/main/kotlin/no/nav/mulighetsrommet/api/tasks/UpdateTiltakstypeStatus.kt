package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class UpdateTiltakstypeStatus(
    slackNotifier: SlackNotifier,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.name, Daily(LocalTime.MIDNIGHT))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke synkronisere tiltakstypestatuser på kafka. Konsekvensen er at statuser på tiltakstyper kan være utdaterte på kafka.")
        }
        .execute { _, context ->
            runBlocking {
                logger.info("Kjører synkronisering av tiltakstypestatuser på kafka")

                val lastSuccessDate = context.execution.lastSuccess
                    ?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
                    ?: LocalDate.of(2023, 2, 1)

                oppdaterTiltakstypestatus(LocalDate.now(), lastSuccessDate)
            }
        }

    fun oppdaterTiltakstypestatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for tiltakstyper med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = DatabaseUtils.paginate(limit = 1000) { paginationParams ->
            val tiltakstyper = tiltakstypeRepository.getAllByDateInterval(
                dateIntervalStart = lastSuccessDate,
                dateIntervalEnd = today,
                pagination = paginationParams,
            )

            tiltakstyper.forEach { it ->
                val eksternTiltakstype = tiltakstypeRepository.getEksternTiltakstype(it.id)
                if (eksternTiltakstype != null) {
                    tiltakstypeKafkaProducer.publish(eksternTiltakstype)
                } else {
                    tiltakstypeKafkaProducer.retract(it.id)
                }
            }

            tiltakstyper
        }
        logger.info("Oppdaterte status for $numberOfUpdates tiltakstyper")
    }
}
