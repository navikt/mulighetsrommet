package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.serialization.json.JsonRelaxExplicitNulls
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.util.*

class SisteTiltaksgjennomforingerV1KafkaConsumer(
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val gjennomforing = JsonRelaxExplicitNulls.decodeFromJsonElement<TiltaksgjennomforingEksternV1Dto?>(message)

        if (gjennomforing == null) {
            queries.gruppetiltak.delete(key)
        } else {
            queries.gruppetiltak.upsert(gjennomforing)
        }
    }
}
