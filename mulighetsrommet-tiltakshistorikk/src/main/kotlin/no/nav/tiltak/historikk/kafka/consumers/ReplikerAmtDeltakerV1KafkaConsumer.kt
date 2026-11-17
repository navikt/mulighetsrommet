package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.historikk.model.KometDeltaker
import no.nav.tiltak.historikk.replikering.KometDeltakerReplikering
import org.slf4j.LoggerFactory
import java.util.UUID

class ReplikerAmtDeltakerV1KafkaConsumer(
    private val replikering: KometDeltakerReplikering,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)
            ?: return replikering.delete(key)

        replikering.upsert(amtDeltaker.toKometDeltaker())
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
    opprettetTidspunkt = registrertDato,
    oppdatertTidspunkt = endretDato,
    dagerPerUke = dagerPerUke,
    prosentStilling = prosentStilling,
)
