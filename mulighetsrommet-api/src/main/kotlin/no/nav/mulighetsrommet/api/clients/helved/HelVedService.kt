package no.nav.mulighetsrommet.api.clients.helved

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling.Periode
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling.Tilskuddstype
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling.Tiltakskode
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HelVedService(
    val config: Config,
    val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val tilskuddUtbetalingTopic: String,
    )

    fun produceTilskuddUtbetalingTest(): UUID? {
        if (NaisEnv.current().isProdGCP()) {
            logger.info("Ignorerer HelVedUtbetaling task for Prod")
            return null
        }
        val vedtakId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val sakId = "2026/test"
        val behandlingId = "$sakId-${vedtakId.mostSignificantBits.toHexString()}" // uuid har 32 karakterer, så får finne på noe annet
        val utbetaling = HelVedUtbetaling(
            id = id,
            sakId = sakId,
            behandlingId = behandlingId, // Maks 30 karakterer, uuid er ellers 32
            personident = NorskIdent("21528416400"), // Gjørme, Proaktiv
            periode = Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
            belop = 1234,
            tilskuddstype = Tilskuddstype.EKSAMENSGEBYR,
            saksbehandler = NavIdent("Z990279"), // Test NavIdent
            beslutter = NavIdent("Z993433"), // Test NavIdent
            besluttetTidspunkt = Instant.now(),
            tiltaksType = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            dryrun = true,
        )
        val record = StoredProducerRecord(
            config.tilskuddUtbetalingTopic,
            utbetaling.id.toString().toByteArray(),
            Json.encodeToString(HelVedUtbetaling.serializer(), utbetaling).toByteArray(),
            null,
        )
        logger.info("Produserer test-utbetaling id=${utbetaling.id} mot Hel Ved på ${config.tilskuddUtbetalingTopic}")

        db.transaction {
            queries.kafkaProducerRecord.storeRecord(record)
        }

        return utbetaling.id
    }
}
