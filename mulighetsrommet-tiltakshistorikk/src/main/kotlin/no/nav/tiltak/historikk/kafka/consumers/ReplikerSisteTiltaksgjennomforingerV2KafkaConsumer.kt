package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.tiltak.historikk.model.Gjennomforing
import no.nav.tiltak.historikk.replikering.GjennomforingReplikering
import java.util.UUID

class ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(
    private val replikering: GjennomforingReplikering,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        val gjennomforing = Json.decodeFromJsonElement<TiltaksgjennomforingV2Dto?>(message)
            ?: return replikering.delete(key)
        replikering.upsert(gjennomforing.toGjennomforing())
    }
}

fun TiltaksgjennomforingV2Dto.toGjennomforing(): Gjennomforing {
    return when (this) {
        is TiltaksgjennomforingV2Dto.Gruppe -> Gjennomforing(
            id = id,
            type = Gjennomforing.Type.GRUPPE,
            tiltakskode = tiltakskode,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer.value,
            navn = navn,
            deltidsprosent = deltidsprosent,
            opprettetTidspunkt = opprettetTidspunkt,
            oppdatertTidspunkt = oppdatertTidspunkt,
        )

        is TiltaksgjennomforingV2Dto.Enkeltplass -> Gjennomforing(
            id = id,
            type = Gjennomforing.Type.ENKELTPLASS,
            tiltakskode = tiltakskode,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer.value,
            navn = null,
            deltidsprosent = null,
            opprettetTidspunkt = opprettetTidspunkt,
            oppdatertTidspunkt = oppdatertTidspunkt,
        )
    }
}
