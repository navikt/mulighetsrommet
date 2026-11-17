package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.contracts.tiltakstype.TiltakstypeV3Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.tiltak.historikk.model.Tiltakstype
import no.nav.tiltak.historikk.replikering.TiltakstypeReplikering
import java.time.ZoneId
import java.util.UUID

class ReplikerSisteTiltakstyperV3KafkaConsumer(
    private val replikering: TiltakstypeReplikering,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        val tiltakstype = Json.decodeFromJsonElement<TiltakstypeV3Dto>(message)
        replikering.upsert(tiltakstype.toTiltakstype())
    }
}

private fun TiltakstypeV3Dto.toTiltakstype(): Tiltakstype = Tiltakstype(
    navn = navn,
    tiltakskode = tiltakskode,
    arenaTiltakskode = tiltakskode.arenakode,
    tiltakstypeId = id,
    opprettetTidspunkt = opprettetTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
    oppdatertTidspunkt = oppdatertTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
)
