package no.nav.mulighetsrommet.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utils.toUUID

class TiltaksgjennomforingTopicConsumer(
    config: Config,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arenaMigreringTiltaksgjennomforingKafkaProducer: ArenaMigreringTiltaksgjennomforingKafkaProducer,
    private val arenaAdapterClient: ArenaAdapterClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        when (val tiltaksgjennomforingDto = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingDto?>(message)) {
            null -> {
                arenaMigreringTiltaksgjennomforingKafkaProducer.retract(key.toUUID())
            }
            else -> {
                val arenaTiltaksgjennomforingDto = arenaAdapterClient.hentArenadata(tiltaksgjennomforingDto.id)
                tiltaksgjennomforingRepository.get(tiltaksgjennomforingDto.id)?.let {
                    val endretTidspunkt = tiltaksgjennomforingRepository.getUpdatedAt(tiltaksgjennomforingDto.id)!!
                    arenaMigreringTiltaksgjennomforingKafkaProducer.publish(
                        ArenaMigreringTiltaksgjennomforingDto.from(
                            it,
                            arenaTiltaksgjennomforingDto?.arenaId,
                            endretTidspunkt,
                        ),
                    )
                }
            }
        }
    }
}
