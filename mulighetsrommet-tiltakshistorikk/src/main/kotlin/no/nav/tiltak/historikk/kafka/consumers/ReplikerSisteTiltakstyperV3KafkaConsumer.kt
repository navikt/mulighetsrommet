package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.TiltakstypeDbo
import java.util.*

class ReplikerSisteTiltakstyperV3KafkaConsumer(
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val tiltakstype = Json.decodeFromJsonElement<TiltakstypeV3Dto>(message)
        queries.tiltakstype.upsert(tiltakstype.toTiltakstypeDbo())
    }
}

private fun TiltakstypeV3Dto.toTiltakstypeDbo(): TiltakstypeDbo = TiltakstypeDbo(
    navn = navn,
    tiltakskode = tiltakskode.name,
    arenaTiltakskode = tiltakskode.arenakode,
)
