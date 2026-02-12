package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class ReplikerDeltakerKafkaConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)

        if (amtDeltaker == null) {
            logger.info("Mottok tombstone for deltaker deltakerId=$key, sletter deltakeren")
            queries.deltaker.get(key)?.let { skedulerOppdaterUtbetalinger(it.gjennomforingId) }
            queries.deltaker.delete(key)
            return
        }

        if (amtDeltaker.status.statusType == DeltakerStatusType.FEILREGISTRERT) {
            logger.info("Sletter deltaker deltakerId=$key fordi den var feilregistrert")
            queries.deltaker.delete(key)
            return skedulerOppdaterUtbetalinger(amtDeltaker.gjennomforingId)
        }

        if (harEndringer(amtDeltaker)) {
            logger.info("Lagrer deltaker deltakerId=$key")
            queries.deltaker.upsert(amtDeltaker.toDeltakerDbo())
            return skedulerOppdaterUtbetalinger(amtDeltaker.gjennomforingId)
        }
    }

    private fun skedulerOppdaterUtbetalinger(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(
            gjennomforingId = gjennomforingId,
            tidspunkt = offsetITilfelleDetErMangeEndringerForGjennomforing,
        )
    }

    private fun QueryContext.harEndringer(amtDeltaker: AmtDeltakerEksternV1Dto): Boolean {
        val deltaker = queries.deltaker.get(amtDeltaker.id) ?: return true

        if (deltaker.endretTidspunkt > amtDeltaker.endretTidspunkt) {
            return false
        }

        return deltaker != Deltaker(
            id = amtDeltaker.id,
            gjennomforingId = amtDeltaker.gjennomforingId,
            startDato = amtDeltaker.startDato,
            sluttDato = amtDeltaker.sluttDato,
            registrertTidspunkt = amtDeltaker.registrertTidspunkt,
            endretTidspunkt = amtDeltaker.endretTidspunkt,
            status = amtDeltaker.status.toDeltakerStatus(),
            deltakelsesmengder = amtDeltaker.deltakelsesmengder.map {
                Deltakelsesmengde(it.gyldigFraDato, it.deltakelsesprosent.toDouble())
            },
        )
    }
}

fun AmtDeltakerEksternV1Dto.toDeltakerDbo(): DeltakerDbo {
    return DeltakerDbo(
        id = id,
        gjennomforingId = gjennomforingId,
        startDato = startDato,
        sluttDato = sluttDato,
        registrertTidspunkt = registrertTidspunkt,
        endretTidspunkt = endretTidspunkt,
        status = status.toDeltakerStatus(),
        deltakelsesmengder = deltakelsesmengder.map {
            DeltakerDbo.Deltakelsesmengde(
                gyldigFra = it.gyldigFraDato,
                opprettetTidspunkt = it.opprettetTidspunkt,
                deltakelsesprosent = it.deltakelsesprosent.toDouble(),
            )
        },
    )
}

private fun AmtDeltakerEksternV1Dto.DeltakerStatusDto.toDeltakerStatus(): DeltakerStatus = DeltakerStatus(
    type = statusType,
    aarsak = aarsakType,
    opprettetTidspunkt = opprettetTidspunkt,
)
