package no.nav.mulighetsrommet.api.utbetaling.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.kafka.TotrinnskontrollHendelseDeserializer
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import org.slf4j.LoggerFactory
import java.util.UUID

class OppdaterUtbetalingBlokkeringerFraBesluttetTilsagnConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<String, TotrinnskontrollHendelse>(
    stringDeserializer(),
    TotrinnskontrollHendelseDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: TotrinnskontrollHendelse) {
        val relevant = when (message.type) {
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollType.TILSAGN_ANNULLERING,
            TotrinnskontrollType.TILSAGN_OPPGJOR,
            -> message.status == TotrinnskontrollHendelse.Status.GODKJENT

            TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
            TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
            TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
            TotrinnskontrollType.TILSKUDD_OPPHOR,
            -> false
        }
        if (relevant) {
            oppdaterUtbetalingBlokkeringerFraBesluttetTilsagn(message.entityId)
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
