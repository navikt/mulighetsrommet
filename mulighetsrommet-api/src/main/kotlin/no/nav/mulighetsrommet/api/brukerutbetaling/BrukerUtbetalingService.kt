package no.nav.mulighetsrommet.api.brukerutbetaling

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BrukerUtbetalingService(
    val config: Config,
    val db: ApiDatabase,
    val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val helvedUtbetalingTopic: String,
    )

    fun handleHelvedStatus(id: UUID, statusMelding: HelVedStatus) {
        logger.info("Melding fra hel ved: {}", Json.encodeToString(statusMelding))
        db.session { queries.brukerUtbetaling.setHelVedStatus(id, statusMelding) }
    }

    fun produceTilskuddUtbetaling(utbetaling: HelVedUtbetaling) {
        val record = ProducerRecord(
            config.helvedUtbetalingTopic,
            utbetaling.id.toString().toByteArray(),
            Json.encodeToString(HelVedUtbetaling.serializer(), utbetaling).toByteArray(),
        )
        logger.info("Produserer utbetaling id=${utbetaling.id} mot Hel Ved på ${config.helvedUtbetalingTopic}")

        kafkaProducerClient.sendSync(record)
    }

    fun produceTilskuddUtbetalingTest(): UUID? {
        if (NaisEnv.current().isProdGCP()) {
            logger.info("Ignorerer HelVedUtbetaling task for Prod")
            return null
        }
        val utbetaling = HelVedUtbetaling(
            id = UUID.randomUUID(),
            sakId = "2026/test",
            behandlingId = "1",
            personIdent = NorskIdent("21528416400"), // Gjørme, Proaktiv
            periode = HelVedUtbetaling.Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
            belop = 1234,
            kostnadssted = NavEnhetNummer("1234"),
            tilskuddstype = HelVedUtbetaling.Tilskuddstype.EKSAMENSGEBYR,
            saksbehandler = NavIdent("Z990279"), // Test NavIdent
            beslutter = NavIdent("Z993433"), // Test NavIdent
            besluttetTidspunkt = Instant.now(),
            tiltakskode = HelVedUtbetaling.Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            dryrun = false,
        )
        produceTilskuddUtbetaling(utbetaling)

        return utbetaling.id
    }
}
