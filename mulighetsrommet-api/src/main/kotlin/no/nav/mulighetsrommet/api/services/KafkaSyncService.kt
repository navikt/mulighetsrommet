package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import org.slf4j.LoggerFactory
import java.time.LocalDate

class KafkaSyncService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun oppdaterTiltaksgjennomforingsstatus(today: LocalDate, lastSuccessDate: LocalDate) {
        var offset = 1
        var count = 0

        logger.info("Oppdaterer statuser for gjennomføringer med start eller sluttdato mellom $lastSuccessDate og $today")
        do {
            val tiltaksgjennomforinger = tiltaksgjennomforingRepository.getAllByDateIntervalAndAvslutningsstatus(
                dateIntervalStartExclusive = lastSuccessDate,
                dateIntervalEndInclusive = today,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                pagination = PaginationParams(nullablePage = offset, nullableLimit = 1000)
            )
            offset += 1

            tiltaksgjennomforinger.forEach { it ->
                tiltaksgjennomforingRepository.get(it.id)?.let {
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(it))
                }
            }

            count += tiltaksgjennomforinger.size
        } while (tiltaksgjennomforinger.isNotEmpty())

        logger.info("Oppdaterte status for $count tiltaksgjennomføringer")
    }
}
/*
Hva er relevante?
avslutningsstatus = IKKE_AVSLUTTET
sluttDato er etter last success
startDato er på eller etter last success

bare startDato er etter last success?

last success = 14.feb
today 16. feb

sluttdato = 15. feb

relevant

sluttdato >= last success

----------
last success = 14.feb
today 16. feb

startDato = 15. feb

relevant

sluttdato >= last success

 */
