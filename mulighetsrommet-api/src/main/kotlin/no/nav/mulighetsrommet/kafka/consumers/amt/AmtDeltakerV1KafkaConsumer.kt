package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import java.util.*

class AmtDeltakerV1KafkaConsumer(
    config: Config,
    private val relevantDeltakerSluttDatoPeriod: Period = Period.ofMonths(3),
    private val tiltakstyper: TiltakstypeService,
    private val deltakere: DeltakerRepository,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            amtDeltaker == null -> {
                logger.info("Mottok tombstone for deltaker deltakerId=$key, sletter deltakeren")
                deltakere.delete(key)
            }

            amtDeltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker deltakerId=$key fordi den var feilregistrert")
                deltakere.delete(key)
            }

            else -> {
                logger.info("Lagrer deltaker deltakerId=$key")
                deltakere.upsert(amtDeltaker.toDeltakerDbo())

                if (isRelevantForRefusjonskrav(amtDeltaker)) {
                    logger.info("Lagrer norsk ident for deltaker som er relevant for refusjonskrav deltakerId=$key")
                    val norskIdent = NorskIdent(amtDeltaker.personIdent)
                    deltakere.setNorskIdent(amtDeltaker.id, norskIdent)
                }
            }
        }
    }

    private fun isRelevantForRefusjonskrav(deltaker: AmtDeltakerV1Dto): Boolean {
        if (NaisEnv.current().isProdGCP()) {
            return false
        }

        val tiltakstype = tiltakstyper.getByGjennomforingId(deltaker.gjennomforingId)
        if (tiltakstype.tiltakskode !in setOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) {
            return false
        }

        if (
            deltaker.status.type !in setOf(
                DeltakerStatus.Type.AVBRUTT,
                DeltakerStatus.Type.DELTAR,
                DeltakerStatus.Type.HAR_SLUTTET,
                DeltakerStatus.Type.FULLFORT,
            )
        ) {
            return false
        }

        val relevantDeltakerSluttDato = LocalDate.now().minus(relevantDeltakerSluttDatoPeriod)
        val sluttDato = deltaker.sluttDato
        return sluttDato == null || !relevantDeltakerSluttDato.isAfter(sluttDato)
    }

    private fun AmtDeltakerV1Dto.toDeltakerDbo(): DeltakerDbo {
        val tiltakstype = tiltakstyper.getByGjennomforingId(gjennomforingId)

        val stillingsprosent = when (tiltakstype.tiltakskode) {
            // Hvis stillingsprosent mangler for AFT/VTA så kan det antas å være 100
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> {
                prosentStilling?.toDouble() ?: 100.0
            }

            // TODO: ikke lese inn stillingsprosent for andre tiltakstyper?
            //  Skal visstnok ikke være relevant for disse, selv om det finnes en del data der per i dag
            else -> prosentStilling?.toDouble()
        }
        return DeltakerDbo(
            id = id,
            gjennomforingId = gjennomforingId,
            startDato = startDato,
            sluttDato = sluttDato,
            registrertTidspunkt = registrertDato,
            endretTidspunkt = endretDato,
            stillingsprosent = stillingsprosent,
            status = status,
        )
    }
}
