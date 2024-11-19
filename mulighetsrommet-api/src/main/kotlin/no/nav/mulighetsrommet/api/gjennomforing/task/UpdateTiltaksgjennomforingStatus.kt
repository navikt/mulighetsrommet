package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.database.utils.DatabaseUtils
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class UpdateTiltaksgjennomforingStatus(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Daily(LocalTime.MIDNIGHT))
        .execute { _, context ->
            val lastSuccessDate = context.execution.lastSuccess
                ?.let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
                ?: LocalDate.now()

            oppdaterTiltaksgjennomforingStatus(LocalDate.now(), lastSuccessDate)
        }

    fun oppdaterTiltaksgjennomforingStatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for gjennomføringer med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = DatabaseUtils.paginate(pageSize = 1000) { pagination ->
            val tiltaksgjennomforinger = tiltaksgjennomforingRepository.getAllByDateIntervalAndNotAvbrutt(
                dateIntervalStart = lastSuccessDate,
                dateIntervalEnd = today,
                pagination = pagination,
            )

            tiltaksgjennomforinger.forEach { id ->
                val gjennomforing = requireNotNull(tiltaksgjennomforingRepository.get(id))
                tiltaksgjennomforingKafkaProducer.publish(gjennomforing.toTiltaksgjennomforingV1Dto())
                if (gjennomforing.status.status == TiltaksgjennomforingStatus.AVSLUTTET) {
                    tiltaksgjennomforingRepository.setPublisert(gjennomforing.id, false)
                }
            }

            tiltaksgjennomforinger
        }
        logger.info("Oppdaterte status for $numberOfUpdates tiltaksgjennomføringer")
    }
}
