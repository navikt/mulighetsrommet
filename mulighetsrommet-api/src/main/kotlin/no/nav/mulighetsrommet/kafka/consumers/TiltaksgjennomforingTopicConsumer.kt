package no.nav.mulighetsrommet.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.*

class TiltaksgjennomforingTopicConsumer(
    config: Config,
    private val tiltakstyper: TiltakstypeService,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val arenaMigreringTiltaksgjennomforingKafkaProducer: ArenaMigreringTiltaksgjennomforingKafkaProducer,
    private val arenaAdapterClient: ArenaAdapterClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingDto?>(message)
            ?: throw UnsupportedOperationException("Arena støtter ikke sletting av gjennomføringer. Tombstone-meldinger er derfor ikke tillatt så lenge data må deles med Arena.")

        if (gjennomforingSkalDelesMedArena(gjennomforing)) {
            publishMigrertGjennomforing(gjennomforing.id)
        }
    }

    private suspend fun publishMigrertGjennomforing(id: UUID) {
        val arenaGjennomforing = arenaAdapterClient.hentArenadata(id)

        val gjennomforing = tiltaksgjennomforingRepository.get(id)
        requireNotNull(gjennomforing)

        val endretTidspunkt = tiltaksgjennomforingRepository.getUpdatedAt(id)
        requireNotNull(endretTidspunkt)

        val migrertGjennomforing = ArenaMigreringTiltaksgjennomforingDto.from(
            gjennomforing,
            arenaGjennomforing?.arenaId,
            endretTidspunkt,
        )
        arenaMigreringTiltaksgjennomforingKafkaProducer.publish(migrertGjennomforing)
    }

    private fun gjennomforingSkalDelesMedArena(gjennomforing: TiltaksgjennomforingDto): Boolean {
        return tiltakstyper.isEnabled(Tiltakskode.fromArenaKode(gjennomforing.tiltakstype.arenaKode))
    }
}
