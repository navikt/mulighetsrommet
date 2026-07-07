package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.getOrElse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.mapper.KategoriseringMapper
import no.nav.mulighetsrommet.api.gjennomforing.service.EnkeltplassRequest
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.UpsertGjennomforingEnkeltplass
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
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
        when (val request = JsonIgnoreUnknownKeys.decodeFromJsonElement<GjennomforingRequest>(message)) {
            is GjennomforingRequest.EnkeltplassUtkast -> handterEnkeltplassUtkast(request)
            is GjennomforingRequest.EnkeltplassSoktInn -> handterEnkeltplassSoktInn(request)
            is GjennomforingRequest.EnkeltplassEndreInnhold -> TODO("Ikke støttet enda")
            is GjennomforingRequest.EnkeltplassEndrePrisinformasjon -> TODO("Ikke støttet enda")
        }
    }

    private suspend fun handterEnkeltplassUtkast(request: GjennomforingRequest.EnkeltplassUtkast) {
        val (gjennomforingId, payload) = request
        validateTiltakskode(payload.tiltakskode)

        val arrangor = getArrangor(payload.organisasjonsnummer)
        val utkast = toRequest(gjennomforingId, arrangor.id, payload)
        enkeltplasser.opprettUtkast(utkast, payload.opprettetAv)
            .onLeft { errors -> error("Klarte ikke opprette enkeltplass: $errors") }
    }

    private suspend fun handterEnkeltplassSoktInn(request: GjennomforingRequest.EnkeltplassSoktInn) {
        val (gjennomforingId, totrinnskontroll, payload) = request
        validateTiltakskode(payload.tiltakskode)

        val arrangor = getArrangor(payload.organisasjonsnummer)
        val soktInn = toRequest(gjennomforingId, arrangor.id, payload)
        enkeltplasser.soktInn(soktInn, totrinnskontroll.behandletAv)
            .onLeft { errors -> error("Klarte ikke opprette enkeltplass: $errors") }
    }

    private fun validateTiltakskode(tiltakskode: Tiltakskode) {
        require(tiltakstyper.erMigrert(tiltakskode)) {
            "Enkeltplass kan bare opprettes når tiltakstypen er migrert"
        }
        require(tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_ENKELTPLASSER)) {
            "Enkeltplass kan bare opprettes for tiltakstyper med støtte for enkeltplasser"
        }
    }

    private suspend fun getArrangor(organisasjonsnummer: Organisasjonsnummer): ArrangorDto = arrangorer
        .getArrangorOrSyncFromBrreg(organisasjonsnummer)
        .getOrElse { error("Klarte ikke hente arrangør fra brreg $it") }
}

private fun toRequest(
    gjennomforingId: UUID,
    arrangorId: UUID,
    payload: UpsertEnkeltplass,
) = EnkeltplassRequest(
    id = gjennomforingId,
    tiltakskode = payload.tiltakskode,
    arrangorId = arrangorId,
    ansvarligEnhet = payload.ansvarligEnhet,
    kategorisering = payload.kategorisering?.let(KategoriseringMapper::fromKafkaPayload),
    prismodell = toPrismodell(payload.prisinformasjon),
)

private fun toPrismodell(
    prisinformasjon: EnkeltplassPrisinformasjon,
): UpsertGjennomforingEnkeltplass.Prismodell {
    return when (prisinformasjon) {
        is EnkeltplassPrisinformasjon.Tilskudd -> UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
            tilskudd = prisinformasjon.tilskudd,
            tilleggsopplysninger = prisinformasjon.tilleggsopplysninger,
        )

        is EnkeltplassPrisinformasjon.Anskaffelse -> UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
            totalbelop = prisinformasjon.pris,
        )

        is EnkeltplassPrisinformasjon.IngenKostnader -> UpsertGjennomforingEnkeltplass.Prismodell.IngenKostnader(
            aarsak = when (prisinformasjon.aarsak) {
                EnkeltplassPrisinformasjon.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT -> Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT
                EnkeltplassPrisinformasjon.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI -> Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI
            },
            tilleggsopplysninger = prisinformasjon.tilleggsopplysninger,
        )
    }
}
