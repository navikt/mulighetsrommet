package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class ReplicateDeltakerKafkaConsumer(
    private val db: ApiDatabase,
    private val oppdaterUtbetaling: OppdaterUtbetalingBeregning,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val deltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        val gjennomforingId: UUID?
        when {
            deltaker == null -> {
                gjennomforingId = queries.deltaker.get(key)?.gjennomforingId
                logger.info("Mottok tombstone for deltaker deltakerId=$key, sletter deltakeren")
                queries.deltaker.delete(key)
            }

            deltaker.status.type == DeltakerStatusType.FEILREGISTRERT -> {
                gjennomforingId = deltaker.gjennomforingId
                logger.info("Sletter deltaker deltakerId=$key fordi den var feilregistrert")
                queries.deltaker.delete(key)
            }

            else -> {
                gjennomforingId = deltaker.gjennomforingId

                logger.info("Lagrer deltaker deltakerId=$key")
                queries.deltaker.upsert(toDeltakerDbo(deltaker))
            }
        }

        gjennomforingId?.let { queries.gjennomforing.getPrismodell(it) }?.run {
            when (type) {
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                -> scheduleOppdateringAvUtbetaling(gjennomforingId)

                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                PrismodellType.ANNEN_AVTALT_PRIS,
                -> Unit
            }
        }
    }

    private fun QueryContext.scheduleOppdateringAvUtbetaling(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        oppdaterUtbetaling.schedule(gjennomforingId, offsetITilfelleDetErMangeEndringerForGjennomforing, session)
    }
}

private fun toDeltakerDbo(deltaker: AmtDeltakerV1Dto): DeltakerDbo {
    return DeltakerDbo(
        id = deltaker.id,
        gjennomforingId = deltaker.gjennomforingId,
        startDato = deltaker.startDato,
        sluttDato = deltaker.sluttDato,
        registrertDato = deltaker.registrertDato.toLocalDate(),
        endretTidspunkt = deltaker.endretDato,
        deltakelsesprosent = deltaker.prosentStilling?.toDouble(),
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
