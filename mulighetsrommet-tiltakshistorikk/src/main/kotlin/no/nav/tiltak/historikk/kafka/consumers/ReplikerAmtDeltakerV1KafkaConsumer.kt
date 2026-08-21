package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.KometDeltaker
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.UUID

class ReplikerAmtDeltakerV1KafkaConsumer(
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            amtDeltaker == null -> {
                logger.info("Mottok tombstone for deltaker med id=$key, sletter deltakeren")
                queries.kometDeltaker.deleteKometDeltaker(key)
            }

            amtDeltaker.status.type == DeltakerStatusType.FEILREGISTRERT -> {
                logger.info("Sletter deltaker med id=$key fordi den var feilregistrert")
                queries.kometDeltaker.deleteKometDeltaker(key)
            }

            else -> {
                val incoming = amtDeltaker.toKometDeltaker()
                val stored = queries.kometDeltaker.get(key)

                if (stored != null && incoming.oppdatertTidspunkt < stored.oppdatertTidspunkt) {
                    logger.info("Hopper over deltaker med id=$key fordi innkommende oppdatertTidspunkt er eldre enn lagret")
                    return@session
                }

                logger.info("Forsøker å lagre deltaker med id=$key")
                query { queries.kometDeltaker.upsertKometDeltaker(incoming) }.onLeft {
                    logger.warn("Feil under konsumering av deltaker med id=$key", it.error)
                    throw it.error
                }
            }
        }
    }
}

fun AmtDeltakerV1Dto.toKometDeltaker() = KometDeltaker(
    id = id,
    gjennomforingId = gjennomforingId,
    personIdent = personIdent,
    startDato = startDato,
    sluttDato = sluttDato,
    statusType = status.type,
    statusOpprettetTidspunkt = status.opprettetDato,
    statusAarsak = status.aarsak,
    opprettetTidspunkt = registrertDato.truncatedTo(ChronoUnit.MICROS),
    oppdatertTidspunkt = endretDato.truncatedTo(ChronoUnit.MICROS),
    dagerPerUke = dagerPerUke,
    prosentStilling = prosentStilling,
)
