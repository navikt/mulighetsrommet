package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.getOrElse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.UpsertGjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import java.util.UUID

class GjennomforingRequestKafkaConsumer(
    private val arrangorer: ArrangorService,
    private val tiltakstyper: TiltakstypeService,
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

        require(tiltakstyper.erMigrert(request.tiltakskode)) {
            "Enkeltplass kan bare opprettes når tiltakstypen er migrert"
        }

        require(request.tiltakskode.harEgenskap(TiltakstypeEgenskap.KAN_OPPRETTE_ENKELTPLASS)) {
            "Enkeltplass kan bare opprettes for tiltakstyper med støttet for enkeltplasser"
        }

        val arrangor = arrangorer
            .getArrangorOrSyncFromBrreg(request.organisasjonsnummer)
            .getOrElse { error("Klarte ikke hente arrangør fra brreg $it") }

        val opprett = UpsertGjennomforingEnkeltplass(
            id = request.gjennomforingId,
            tiltakskode = request.tiltakskode,
            arrangorId = arrangor.id,
            status = GjennomforingStatusType.GJENNOMFORES,
            prisbetingelser = request.prisinformasjon,
            deltidsprosent = 100.0,
            antallPlasser = 1,
            ansvarligEnhet = request.ansvarligEnhet,
            navn = null,
            startDato = null,
            sluttDato = null,
            arenaTiltaksnummer = null,
            arenaAnsvarligEnhet = null,
        )
        enkeltplasser.create(opprett, request.opprettetAv)
    }
}
