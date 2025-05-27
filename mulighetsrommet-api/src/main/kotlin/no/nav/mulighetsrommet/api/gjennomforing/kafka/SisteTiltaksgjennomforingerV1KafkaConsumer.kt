package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.*

class SisteTiltaksgjennomforingerV1KafkaConsumer(
    config: Config,
    private val db: ApiDatabase,
    private val tiltakstyper: TiltakstypeService,
    private val arenaMigreringTiltaksgjennomforingProducer: ArenaMigreringTiltaksgjennomforingerV1KafkaProducer,
    private val arenaAdapterClient: ArenaAdapterClient,
) : KafkaTopicConsumer<String, JsonElement>(
    config,
    stringDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: String, message: JsonElement) {
        val gjennomforing = JsonIgnoreUnknownKeys.decodeFromJsonElement<TiltaksgjennomforingEksternV1Dto?>(message)
            ?: throw UnsupportedOperationException("Arena støtter ikke sletting av gjennomføringer. Tombstone-meldinger er derfor ikke tillatt så lenge data må deles med Arena.")

        if (gjennomforingSkalDelesMedArena(gjennomforing)) {
            publishMigrertGjennomforing(gjennomforing.id)
        }
    }

    private suspend fun publishMigrertGjennomforing(id: UUID): Unit = db.session {
        val arenaGjennomforing = arenaAdapterClient.hentArenadata(id)

        val gjennomforing = queries.gjennomforing.get(id)
        requireNotNull(gjennomforing)

        val migrertGjennomforing = ArenaMigreringTiltaksgjennomforingDto.from(
            gjennomforing,
            arenaGjennomforing?.arenaId,
        )
        arenaMigreringTiltaksgjennomforingProducer.publish(migrertGjennomforing)
    }

    private fun gjennomforingSkalDelesMedArena(gjennomforing: TiltaksgjennomforingEksternV1Dto): Boolean {
        return tiltakstyper.isEnabled(gjennomforing.tiltakstype.tiltakskode)
    }
}
