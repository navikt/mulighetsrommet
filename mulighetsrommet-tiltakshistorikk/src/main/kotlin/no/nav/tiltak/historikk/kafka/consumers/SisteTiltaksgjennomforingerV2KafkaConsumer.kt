package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.serialization.json.JsonRelaxExplicitNulls
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.GjennomforingDbo
import no.nav.tiltak.historikk.db.queries.GjennomforingType
import java.util.*

class SisteTiltaksgjennomforingerV2KafkaConsumer(
    private val db: TiltakshistorikkDatabase,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        val gjennomforing = JsonRelaxExplicitNulls.decodeFromJsonElement<TiltaksgjennomforingV2Dto?>(message)

        if (gjennomforing == null) {
            queries.gjennomforing.delete(key)
        } else {
            queries.gjennomforing.upsert(toGjennomforingDbo(gjennomforing))
        }
    }
}

fun toGjennomforingDbo(gjennomforing: TiltaksgjennomforingV2Dto): GjennomforingDbo {
    return when (gjennomforing) {
        is TiltaksgjennomforingV2Dto.Gruppe -> GjennomforingDbo(
            id = gjennomforing.id,
            type = GjennomforingType.GRUPPE,
            tiltakskode = gjennomforing.tiltakskode,
            arrangorOrganisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer.value,
            navn = gjennomforing.navn,
            deltidsprosent = gjennomforing.deltidsprosent,
        )

        is TiltaksgjennomforingV2Dto.Enkeltplass -> GjennomforingDbo(
            id = gjennomforing.id,
            type = GjennomforingType.ENKELTPLASS,
            tiltakskode = gjennomforing.tiltakskode,
            arrangorOrganisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer.value,
            navn = null,
            deltidsprosent = null,
        )
    }
}
