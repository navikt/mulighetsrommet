package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.getOrElse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingRequestPayload
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.UpsertGjennomforingEnkeltplass
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.time.LocalDate
import java.util.UUID

class GjennomforingRequestKafkaConsumer(
    private val arrangorer: ArrangorService,
    private val enkeltplasser: GjennomforingEnkeltplassService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        when (val request = JsonIgnoreUnknownKeys.decodeFromJsonElement<GjennomforingRequestPayload>(message)) {
            is GjennomforingRequestPayload.OpprettEnkeltplass -> opprettGjennomforingEnkeltplass(request)
        }
    }

    private suspend fun opprettGjennomforingEnkeltplass(request: GjennomforingRequestPayload.OpprettEnkeltplass) {
        if (enkeltplasser.get(request.gjennomforingId) != null) {
            return
        }

        val arrangor = arrangorer
            .getArrangorOrSyncFromBrreg(Organisasjonsnummer(request.organisasjonsnummer))
            .getOrElse { error("Klarte ikke hente arrangør fra brreg $it") }

        val opprett = UpsertGjennomforingEnkeltplass(
            id = request.gjennomforingId,
            tiltakskode = request.tiltakskode,
            arrangorId = arrangor.id,
            navn = null,
            startDato = LocalDate.now(),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
            prisbetingelser = request.prisinformasjon,
            deltidsprosent = 100.0,
            antallPlasser = 1,
            arenaTiltaksnummer = null,
            arenaAnsvarligEnhet = null,
        )
        enkeltplasser.create(opprett)
    }
}
