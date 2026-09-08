package no.nav.mulighetsrommet.api.utbetaling.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class OppdaterUtbetalingBeregningEtterReturnertUtbetalingConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<String, TotrinnskontrollHendelse>(
    stringDeserializer(),
    TotrinnskontrollHendelseDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: TotrinnskontrollHendelse) {
        val kanPavirkeBeregning = when (message.type) {
            TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
            -> message.status == TotrinnskontrollHendelse.Status.RETURNERT

            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollType.TILSAGN_ANNULLERING,
            TotrinnskontrollType.TILSAGN_OPPGJOR,
            TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
            TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
            -> false
        }
        if (kanPavirkeBeregning) {
            oppdaterUtbetalingBeregningFraReturnertLinje(message.entityId)
        }
    }

    fun oppdaterUtbetalingBeregningFraReturnertLinje(utbetalingLinjeId: UUID) {
        val linje = db.session { queries.utbetalingLinje.get(utbetalingLinjeId) } ?: return
        val utbetaling = db.session { queries.utbetaling.get(linje.utbetalingId) } ?: return
        val gjennomforingId = utbetaling.gjennomforing.id

        logger.info("Utbetalingslinje $utbetalingLinjeId ble returnert, trigger oppdatering av utbetalinger for gjennomføring $gjennomforingId i tilfelle grunnlaget har endret seg")

        val offsetITilfelleFlereLinjeBleReturnertForSammeUtbetaling = Instant.now().plusSeconds(5)
        genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(
            gjennomforingId = gjennomforingId,
            tidspunkt = offsetITilfelleFlereLinjeBleReturnertForSammeUtbetaling,
        )
    }
}
