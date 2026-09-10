package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelseOld
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.UUID

class OppdaterUtbetalingBlokkeringerFraBesluttetTilsagnConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement) {
        val totrinnskontrollHendelse = try {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse>(message)
        } catch (_: Throwable) {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelseOld>(message).toNew()
        }

        val relevant = when (totrinnskontrollHendelse.type) {
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollType.TILSAGN_ANNULLERING,
            TotrinnskontrollType.TILSAGN_OPPGJOR,
            -> totrinnskontrollHendelse.status == TotrinnskontrollHendelse.Status.GODKJENT

            TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
            TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
            TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
            TotrinnskontrollType.TILSKUDD_OPPHOR,
            -> false
        }
        if (relevant) {
            oppdaterUtbetalingBlokkeringerFraBesluttetTilsagn(totrinnskontrollHendelse.entityId)
        }
    }

    fun oppdaterUtbetalingBlokkeringerFraBesluttetTilsagn(tilsagnId: UUID) {
        val tilsagn = db.session { queries.tilsagn.getOrError(tilsagnId) }
        logger.info("Tilsagn $tilsagnId besluttet, oppdaterer utbetaling blokkeringer for gjennomforing ${tilsagn.gjennomforing.id}")
        genererUtbetalingService.oppdaterUtbetalingBlokkeringerForGjennomforing(
            gjennomforingId = tilsagn.gjennomforing.id,
        )
    }
}
