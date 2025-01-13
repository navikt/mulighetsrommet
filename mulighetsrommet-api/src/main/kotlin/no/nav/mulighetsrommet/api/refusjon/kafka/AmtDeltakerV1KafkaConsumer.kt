package no.nav.mulighetsrommet.api.refusjon.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.refusjon.RefusjonService
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerDbo
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.database.Database
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
    private val db: Database,
    private val tiltakstyper: TiltakstypeService,
    private val refusjonService: RefusjonService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) = db.tx {
        logger.info("Konsumerer deltaker med id=$key")

        val deltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            deltaker == null -> {
                logger.info("Mottok tombstone for deltaker deltakerId=$key, sletter deltakeren")
                Queries.deltaker.delete(key)
            }

            deltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker deltakerId=$key fordi den var feilregistrert")
                Queries.deltaker.delete(key)
            }

            else -> {
                logger.info("Lagrer deltaker deltakerId=$key")
                Queries.deltaker.upsert(deltaker.toDeltakerDbo())

                if (isRelevantForRefusjonskrav(deltaker)) {
                    Queries.deltaker.setNorskIdent(deltaker.id, NorskIdent(deltaker.personIdent))

                    refusjonService.recalculateRefusjonskravForGjennomforing(deltaker.gjennomforingId)
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

        val deltakelsesprosent = when (tiltakstype.tiltakskode) {
            // Hvis deltakelsesprosent mangler for AFT/VTA så skal det antas å være 100
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> {
                prosentStilling?.toDouble() ?: 100.0
            }

            else -> null
        }
        return DeltakerDbo(
            id = id,
            gjennomforingId = gjennomforingId,
            startDato = startDato,
            sluttDato = sluttDato,
            registrertTidspunkt = registrertDato,
            endretTidspunkt = endretDato,
            deltakelsesprosent = deltakelsesprosent,
            status = status,
        )
    }
}
