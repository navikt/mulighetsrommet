package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginate
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KafkaSyncService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun oppdaterTiltaksgjennomforingsstatus(today: LocalDate, lastSuccessDate: LocalDate) {
        logger.info("Oppdaterer statuser for gjennomføringer med start eller sluttdato mellom $lastSuccessDate og $today")

        val numberOfUpdates = paginate(limit = 1000) { paginationParams ->
            val tiltaksgjennomforinger = tiltaksgjennomforingRepository.getAllByDateIntervalAndAvslutningsstatus(
                dateIntervalStart = lastSuccessDate,
                dateIntervalEnd = today,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                pagination = paginationParams
            ).getOrThrow()

            tiltaksgjennomforinger.forEach { it ->
                tiltaksgjennomforingRepository.get(it.id).getOrThrow()
                    ?.let {
                        tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
                    }
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
                pagination = paginationParams
            )

            tiltakstyper.forEach { it ->
                tiltakstypeRepository.get(it.id)?.let {
                    tiltakstypeKafkaProducer.publish(it)
                }
            }

            tiltakstyper
        }
        logger.info("Oppdaterte status for $numberOfUpdates tiltakstyper")
    }
}
