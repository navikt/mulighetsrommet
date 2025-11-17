package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.GjennomforingDbo
import no.nav.tiltak.historikk.db.queries.GjennomforingType
import no.nav.tiltak.historikk.service.VirksomhetService
import java.util.*

class SisteTiltaksgjennomforingerV2KafkaConsumer(
    private val db: TiltakshistorikkDatabase,
    private val virksomheter: VirksomhetService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        val gjennomforing = Json.decodeFromJsonElement<TiltaksgjennomforingV2Dto?>(message)
            ?: return deleteGjennomforing(key)
        syncVirksomhet(gjennomforing.arrangor.organisasjonsnummer)
        upsertGjennomforing(gjennomforing)
    }

    private fun deleteGjennomforing(id: UUID): Unit = db.session {
        queries.gjennomforing.delete(id)
    }

    private suspend fun syncVirksomhet(organisasjonsnummer: Organisasjonsnummer) {
        virksomheter.syncVirksomhetIfNotExists(organisasjonsnummer)
    }

    private fun upsertGjennomforing(gjennomforing: TiltaksgjennomforingV2Dto): Unit = db.session {
        val dbo = toGjennomforingDbo(gjennomforing)
        queries.gjennomforing.upsert(dbo)
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
