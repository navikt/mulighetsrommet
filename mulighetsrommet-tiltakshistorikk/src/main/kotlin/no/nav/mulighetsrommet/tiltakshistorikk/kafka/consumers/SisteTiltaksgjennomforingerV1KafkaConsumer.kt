package no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonRelaxExplicitNulls
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import java.util.*

class SisteTiltaksgjennomforingerV1KafkaConsumer(
    config: Config,
    private val gruppetiltakRepository: GruppetiltakRepository,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        val gjennomforing = JsonRelaxExplicitNulls.decodeFromJsonElement<TiltaksgjennomforingEksternV1Dto?>(message)

        if (gjennomforing == null) {
            gruppetiltakRepository.delete(key)
        } else {
            gruppetiltakRepository.upsert(gjennomforing)
        }
    }
}
