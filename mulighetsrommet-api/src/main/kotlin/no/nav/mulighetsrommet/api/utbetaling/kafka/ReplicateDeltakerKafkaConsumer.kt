package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.task.OppdaterUtbetalingBeregning
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.*

class ReplicateDeltakerKafkaConsumer(
    private val relevantDeltakerSluttDatoPeriod: Period = Period.ofMonths(3),
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
                val prismodell = queries.gjennomforing.getPrismodell(deltaker.gjennomforingId)

                logger.info("Lagrer deltaker deltakerId=$key")
                queries.deltaker.upsert(toDeltakerDbo(deltaker, prismodell))

                if (prismodell != null && isRelevantForUtbetaling(deltaker, prismodell)) {
                    queries.deltaker.setNorskIdent(deltaker.id, NorskIdent(deltaker.personIdent))
                }
            }
        }
        if (gjennomforingId != null) {
            scheduleOppdateringAvUtbetaling(gjennomforingId)
        }
    }

    private fun isRelevantForUtbetaling(deltaker: AmtDeltakerV1Dto, prismodell: Prismodell): Boolean {
        when (prismodell) {
            Prismodell.ANNEN_AVTALT_PRIS -> return false

            Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
            Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
            Prismodell.AVTALT_PRIS_PER_UKESVERK,
            -> Unit
        }

        if (
            deltaker.status.type !in setOf(
                DeltakerStatusType.AVBRUTT,
                DeltakerStatusType.DELTAR,
                DeltakerStatusType.HAR_SLUTTET,
                DeltakerStatusType.FULLFORT,
            )
        ) {
            return false
        }

        val relevantDeltakerSluttDato = LocalDate.now().minus(relevantDeltakerSluttDatoPeriod)
        val sluttDato = deltaker.sluttDato
        return sluttDato == null || !relevantDeltakerSluttDato.isAfter(sluttDato)
    }

    private fun QueryContext.scheduleOppdateringAvUtbetaling(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        oppdaterUtbetaling.schedule(gjennomforingId, offsetITilfelleDetErMangeEndringerForGjennomforing, session)
    }
}

private fun toDeltakerDbo(deltaker: AmtDeltakerV1Dto, prismodell: Prismodell?): DeltakerDbo {
    val deltakelsesprosent = when (prismodell) {
        // Hvis deltakelsesprosent mangler for forhåndsgodkjente tiltak så skal det antas å være 100%
        Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> deltaker.prosentStilling?.toDouble() ?: 100.0
        else -> null
    }
    return DeltakerDbo(
        id = deltaker.id,
        gjennomforingId = deltaker.gjennomforingId,
        startDato = deltaker.startDato,
        sluttDato = deltaker.sluttDato,
        registrertDato = deltaker.registrertDato.toLocalDate(),
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
