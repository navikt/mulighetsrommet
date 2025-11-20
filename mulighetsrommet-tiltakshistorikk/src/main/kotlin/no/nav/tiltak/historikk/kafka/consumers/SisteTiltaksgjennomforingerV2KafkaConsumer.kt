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
        virksomheter.getOrSyncVirksomhetIfNotExists(organisasjonsnummer).onLeft { error ->
            throw IllegalStateException("Forventet å finne virksomhet med orgnr=$organisasjonsnummer i Brreg. Er orgnr gyldig? Error: $error")
        }
    }

    private fun upsertGjennomforing(gjennomforing: TiltaksgjennomforingV2Dto): Unit = db.session {
        val dbo = gjennomforing.toGjennomforingDbo()
        queries.gjennomforing.upsert(dbo)
    }
}

fun TiltaksgjennomforingV2Dto.toGjennomforingDbo(): GjennomforingDbo {
    return when (this) {
        is TiltaksgjennomforingV2Dto.Gruppe -> GjennomforingDbo(
            id = id,
            type = GjennomforingType.GRUPPE,
            tiltakskode = tiltakskode,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer.value,
            navn = navn,
            deltidsprosent = deltidsprosent,
        )

        is TiltaksgjennomforingV2Dto.Enkeltplass -> GjennomforingDbo(
            id = id,
            type = GjennomforingType.ENKELTPLASS,
            tiltakskode = tiltakskode,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer.value,
            navn = null,
            deltidsprosent = null,
        )
    }
}
