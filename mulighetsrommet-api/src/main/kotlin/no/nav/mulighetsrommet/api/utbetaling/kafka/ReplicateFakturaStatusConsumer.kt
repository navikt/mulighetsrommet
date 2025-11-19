package no.nav.mulighetsrommet.api.utbetaling.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.FakturaStatus
import no.nav.tiltak.okonomi.FakturaStatusType
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
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
            oppdaterUtbetaling(it.utbetalingId)
        }
    }

    fun TransactionalQueryContext.oppdaterUtbetaling(utbetalingId: UUID) {
        val utbetaling = queries.utbetaling.getOrError(utbetalingId)
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetalingId)

        val oppdatertUtbetalingStatus = when {
            delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.UTBETALT
            delutbetalinger.any { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.DELVIS_UTBETALT
            else -> utbetaling.status
        }
        if (utbetaling.status != oppdatertUtbetalingStatus) {
            oppdaterUtbetalingStatus(utbetaling.id, oppdatertUtbetalingStatus)
        }
    }

    fun TransactionalQueryContext.oppdaterUtbetalingStatus(utbetalingId: UUID, nyStatus: UtbetalingStatusType) {
        queries.utbetaling.setStatus(utbetalingId, nyStatus)
        val utbetaling = queries.utbetaling.getOrError(utbetalingId)

        val endringsMelding = when (nyStatus) {
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.INNSENDT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            -> "Status endret"
            UtbetalingStatusType.DELVIS_UTBETALT -> "Delvis utbetalt"
            UtbetalingStatusType.UTBETALT -> "Utbetalt"
        }
        logEndring(endringsMelding, utbetaling, Tiltaksadministrasjon)
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        dto: Utbetaling,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }
}
