package no.nav.mulighetsrommet.api.utbetaling.kafka

import arrow.core.some
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.FakturaStatus
import no.nav.tiltak.okonomi.FakturaStatusType
import org.slf4j.LoggerFactory
import java.util.UUID

class ReplicateFakturaStatusConsumer(
    private val db: ApiDatabase,
) : KafkaTopicConsumer<String, JsonElement>(
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: String, message: JsonElement): Unit = db.transaction {
        logger.info("Konsumerer statusmelding fakturanummer=$key")

        val (fakturanummer, status, fakturaStatusSistOppdatert) =
            JsonIgnoreUnknownKeys.decodeFromJsonElement<FakturaStatus>(message)

        when (status) {
            FakturaStatusType.FEILET,
            FakturaStatusType.SENDT,
            FakturaStatusType.IKKE_BETALT,
            -> queries.delutbetaling.setStatus(fakturanummer, DelutbetalingStatus.OVERFORT_TIL_UTBETALING)

            FakturaStatusType.DELVIS_BETALT,
            FakturaStatusType.FULLT_BETALT,
            -> queries.delutbetaling.setStatus(fakturanummer, DelutbetalingStatus.UTBETALT)
        }

        queries.delutbetaling.setFakturaStatus(fakturanummer, status, fakturaStatusSistOppdatert)
        queries.delutbetaling.getOrError(fakturanummer).let {
            oppdaterUtbetalingStatus(it.utbetalingId)
        }
    }

    fun TransactionalQueryContext.oppdaterUtbetalingStatus(utbetalingId: UUID) {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetalingId)
        val utbetalingStatus = when {
            delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.UTBETALT
            delutbetalinger.any { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.DELVIS_UTBETALT
            else -> null
        }
        utbetalingStatus?.let {
            queries.utbetaling.setStatus(utbetalingId, it)
        }
    }
}
