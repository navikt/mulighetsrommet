package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.mapper.KategoriseringMapper
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.UpsertGjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.UUID

class GjennomforingRequestKafkaConsumer(
    private val arrangorer: ArrangorService,
    private val tiltakstyper: TiltakstypeService,
    private val enkeltplasser: GjennomforingEnkeltplassService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

        require(tiltakstyper.erMigrert(payload.tiltakskode)) {
            "Enkeltplass kan bare opprettes når tiltakstypen er migrert"
        }

        require(payload.tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_ENKELTPLASSER)) {
            "Enkeltplass kan bare opprettes for tiltakstyper med støtte for enkeltplasser"
        }

        if (enkeltplasser.get(gjennomforingId) != null) {
            log.info("Enkeltplass er allerede opprettet")
            return
        }

        val arrangor = getArrangor(payload.organisasjonsnummer)
        val prismodell = toPrismodell(UUID.randomUUID(), payload.prisinformasjon)
        val opprett = UpsertGjennomforingEnkeltplass(
            id = gjennomforingId,
            tiltakskode = payload.tiltakskode,
            arrangorId = arrangor.id,
            status = GjennomforingStatusType.GJENNOMFORES,
            ansvarligEnhet = payload.ansvarligEnhet,
            kategorisering = payload.kategorisering?.let(KategoriseringMapper::fromKafkaPayload),
            prismodell = prismodell,
        )
        enkeltplasser.upsert(opprett)
            .getOrElse { errors -> error("Klarte ikke opprette enkeltplass: $errors") }
    }

    private suspend fun handterEnkeltplassSoktInn(request: GjennomforingRequest.EnkeltplassSoktInn) {
        val (gjennomforingId, payload) = request

        require(tiltakstyper.erMigrert(payload.tiltakskode)) {
            "Enkeltplass kan bare opprettes når tiltakstypen er migrert"
        }

        require(payload.tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_ENKELTPLASSER)) {
            "Enkeltplass kan bare opprettes for tiltakstyper med støtte for enkeltplasser"
        }

        val enkeltplass = enkeltplasser.get(gjennomforingId)
        if (enkeltplass?.okonomi != null) {
            log.info("Enkeltplass er allerede søkt inn")
            return
        }

        val arrangor = getArrangor(payload.organisasjonsnummer)
        val prismodell = toPrismodell(
            enkeltplass?.gjennomforing?.prismodell?.id ?: UUID.randomUUID(),
            payload.prisinformasjon,
        )
        val upsert = UpsertGjennomforingEnkeltplass(
            id = gjennomforingId,
            tiltakskode = payload.tiltakskode,
            arrangorId = arrangor.id,
            status = GjennomforingStatusType.GJENNOMFORES,
            ansvarligEnhet = payload.ansvarligEnhet,
            kategorisering = payload.kategorisering?.let(KategoriseringMapper::fromKafkaPayload),
            prismodell = prismodell,
        )

        enkeltplasser.upsert(upsert)
            .flatMap { enkeltplasser.settOkonomiTilGodkjenning(it.id, payload.opprettetAv) }
            .getOrElse { errors -> error("Klarte ikke opprette enkeltplass: $errors") }
    }

    private suspend fun getArrangor(organisasjonsnummer: Organisasjonsnummer): ArrangorDto = arrangorer
        .getArrangorOrSyncFromBrreg(organisasjonsnummer)
        .getOrElse { error("Klarte ikke hente arrangør fra brreg $it") }
}

private fun toPrismodell(
    id: UUID,
    prisinformasjon: EnkeltplassPrisinformasjon,
): Prismodell {
    return when (prisinformasjon) {
        is EnkeltplassPrisinformasjon.Tilskudd -> Prismodell.TilskuddTilOpplaering(
            id = id,
            valuta = Valuta.NOK,
            tilskudd = prisinformasjon.tilskudd,
            tilleggsopplysninger = prisinformasjon.tilleggsopplysninger,
        )

        is EnkeltplassPrisinformasjon.Anskaffelse -> Prismodell.AnnenAvtaltPris(
            id = id,
            valuta = Valuta.NOK,
            tilsagnPerDeltaker = true,
            prisbetingelser = null,
            totalbelop = prisinformasjon.pris,
        )

        is EnkeltplassPrisinformasjon.IngenKostnader -> Prismodell.IngenKostnader(
            id = id,
            valuta = Valuta.NOK,
            aarsak = when (prisinformasjon.aarsak) {
                EnkeltplassPrisinformasjon.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT -> Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_EGENFINANSIERT
                EnkeltplassPrisinformasjon.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI -> Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI
            },
            tilleggsopplysninger = prisinformasjon.tilleggsopplysninger,
        )
    }
}
