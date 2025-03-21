package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import java.util.*

class AmtDeltakerV1KafkaConsumer(
    config: Config,
    private val relevantDeltakerSluttDatoPeriod: Period = Period.ofMonths(3),
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer deltaker med id=$key")

        val deltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            deltaker == null -> {
                logger.info("Mottok tombstone for deltaker deltakerId=$key, sletter deltakeren")
                queries.deltaker.delete(key)
            }

            deltaker.status.type == DeltakerStatus.Type.FEILREGISTRERT -> {
                logger.info("Sletter deltaker deltakerId=$key fordi den var feilregistrert")
                queries.deltaker.delete(key)
            }

            else -> {
                val prismodell = queries.gjennomforing.getPrismodell(deltaker.gjennomforingId)

                logger.info("Lagrer deltaker deltakerId=$key")
                queries.deltaker.upsert(toDeltakerDbo(deltaker, prismodell))

                if (prismodell != null && isRelevantForUtbetaling(deltaker, prismodell)) {
                    queries.deltaker.setNorskIdent(deltaker.id, NorskIdent(deltaker.personIdent))

                    utbetalingService.recalculateUtbetalingForGjennomforing(deltaker.gjennomforingId)
                }
            }
        }
    }

    // TODO: oppdater logikk ifm. prodsetting av tiltaksøkonomi
    private fun isRelevantForUtbetaling(deltaker: AmtDeltakerV1Dto, prismodell: Prismodell): Boolean {
        if (NaisEnv.current().isProdGCP()) {
            return false
        }

        when (prismodell) {
            Prismodell.FRI -> return false
            Prismodell.FORHANDSGODKJENT -> Unit
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
}

private fun toDeltakerDbo(deltaker: AmtDeltakerV1Dto, prismodell: Prismodell?): DeltakerDbo {
    val deltakelsesprosent = when (prismodell) {
        // Hvis deltakelsesprosent mangler for forhåndsgodkjente tiltak så skal det antas å være 100%
        Prismodell.FORHANDSGODKJENT -> deltaker.prosentStilling?.toDouble() ?: 100.0
        Prismodell.FRI, null -> null
    }
    return DeltakerDbo(
        id = deltaker.id,
        gjennomforingId = deltaker.gjennomforingId,
        startDato = deltaker.startDato,
        sluttDato = deltaker.sluttDato,
        registrertTidspunkt = deltaker.registrertDato,
        endretTidspunkt = deltaker.endretDato,
        deltakelsesprosent = deltakelsesprosent,
        status = deltaker.status,
        deltakelsesmengder = deltaker.deltakelsesmengder
            ?.map {
                DeltakerDbo.Deltakelsesmengde(
                    gyldigFra = it.gyldigFra,
                    opprettetTidspunkt = it.opprettet,
                    deltakelsesprosent = it.deltakelsesprosent.toDouble(),
                )
            }
            ?: listOf(),
    )
}
