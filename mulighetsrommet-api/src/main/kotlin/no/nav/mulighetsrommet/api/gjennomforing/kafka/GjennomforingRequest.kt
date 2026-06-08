package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringResponse
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
sealed interface GjennomforingRequest {
    val gjennomforingId: UUID

    @Serializable
    @SerialName("EnkeltplassUtkast")
    data class EnkeltplassUtkast(
        @Serializable(with = UUIDSerializer::class)
        override val gjennomforingId: UUID,
        val payload: UpsertEnkeltplass,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassSoktInn")
    data class EnkeltplassSoktInn(
        @Serializable(with = UUIDSerializer::class)
        override val gjennomforingId: UUID,
        val payload: UpsertEnkeltplass,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassEndrePrisinformasjon")
    data class EnkeltplassEndrePrisinformasjon(
        @Serializable(with = UUIDSerializer::class)
        override val gjennomforingId: UUID,
        val payload: EnkeltplassPrisinformasjon,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassEndreInnhold")
    data class EnkeltplassEndreInnhold(
        @Serializable(with = UUIDSerializer::class)
        override val gjennomforingId: UUID,
        val payload: OpplaringKategorisering?,
    ) : GjennomforingRequest
}

@Serializable
data class UpsertEnkeltplass(
    val tiltakskode: Tiltakskode,
    val organisasjonsnummer: Organisasjonsnummer,
    val ansvarligEnhet: NavEnhetNummer,
    val opprettetAv: NavIdent,
    val prisinformasjon: EnkeltplassPrisinformasjon,
    val kategorisering: OpplaringKategorisering?,
)

@Serializable
sealed interface EnkeltplassPrisinformasjon {
    @Serializable
    @SerialName("EnkeltplassPrisinformasjonAnskaffelse")
    data class Anskaffelse(
        val pris: Int,
    ) : EnkeltplassPrisinformasjon

    @Serializable
    @SerialName("EnkeltplassPrisinformasjonTilskudd")
    data class Tilskudd(
        val tilskudd: Map<Opplaeringtilskudd.Kode, Int>,
        val tilleggsopplysninger: String?,
    ) : EnkeltplassPrisinformasjon

    @Serializable
    @SerialName("EnkeltplassPrisinformasjonIngenKostnader")
    data class IngenKostnader(
        val aarsak: Aarsak,
        val tilleggsopplysninger: String?,
    ) : EnkeltplassPrisinformasjon {
        enum class Aarsak {
            OPPLAERINGEN_ER_KOSTNADSFRI,
            OPPLAERINGEN_ER_EGENFINANSIERT,
        }
    }
}

@Serializable
data class OpplaringKategorisering(
    val verdier: Map<
        OpplaringKategoriseringResponse.Representerer,
        List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >,
        >,
    val sertifiseringer: List<SertifiseringValg>,
) {
    @Serializable
    data class SertifiseringValg(
        val id: Long,
        val navn: String,
    )
}
