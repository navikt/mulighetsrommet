package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginate
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KafkaSyncService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun oppdaterTiltaksgjennomforingStatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for gjennomføringer med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = paginate(limit = 1000) { paginationParams ->
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

    fun oppdaterTiltakstypestatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for tiltakstyper med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = paginate(limit = 1000) { paginationParams ->
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
