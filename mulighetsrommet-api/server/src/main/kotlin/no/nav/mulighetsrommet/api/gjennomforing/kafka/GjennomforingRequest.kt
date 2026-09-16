@file:UseSerializers(UUIDSerializer::class, LocalDateSerializer::class)

package no.nav.mulighetsrommet.api.gjennomforing.kafka

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringResponse
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed interface GjennomforingRequest {
    val gjennomforingId: UUID

    @Serializable
    @SerialName("EnkeltplassUtkast")
    data class EnkeltplassUtkast(
        override val gjennomforingId: UUID,
        val payload: UpsertEnkeltplass,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassSoktInn")
    data class EnkeltplassSoktInn(
        override val gjennomforingId: UUID,
        val totrinnskontroll: Totrinnskontroll,
        val payload: UpsertEnkeltplass,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassEndrePrisinformasjon")
    data class EnkeltplassEndrePrisinformasjon(
        override val gjennomforingId: UUID,
        val totrinnskontroll: Totrinnskontroll,
        val payload: EnkeltplassPrisinformasjon,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassEndreInnhold")
    data class EnkeltplassEndreInnhold(
        override val gjennomforingId: UUID,
        val payload: OpplaringKategorisering?,
    ) : GjennomforingRequest

    @Serializable
    @SerialName("EnkeltplassTilbakekallPrisinformasjon")
    data class EnkeltplassTilbakekallPrisinformasjon(
        override val gjennomforingId: UUID,
        val totrinnskontroll: Totrinnskontroll,
    ) : GjennomforingRequest

    @Serializable
    data class UpsertEnkeltplass(
        val tiltakskode: Tiltakskode,
        val organisasjonsnummer: Organisasjonsnummer,
        val ansvarligEnhet: NavEnhetNummer,
        val startDato: LocalDate,
        val sluttDato: LocalDate,
        val prisinformasjon: EnkeltplassPrisinformasjon,
        val kategorisering: OpplaringKategorisering?,
        val opprettetAv: NavIdent,
    )

    @Serializable
    data class Totrinnskontroll(
        val id: UUID,
        val behandletAv: NavIdent,
    )

    @Serializable
    sealed interface EnkeltplassPrisinformasjon {
        @Serializable
        @SerialName("Anskaffelse")
        data class Anskaffelse(
            val pris: Int,
        ) : EnkeltplassPrisinformasjon

        @Serializable
        @SerialName("Tilskudd")
        data class Tilskudd(
            val tilskudd: Map<Opplaeringtilskudd.Kode, Int>,
            val tilleggsopplysninger: String?,
        ) : EnkeltplassPrisinformasjon

        @Serializable
        @SerialName("IngenKostnader")
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
        val verdier: Map<OpplaringKategoriseringResponse.Representerer, List<UUID>>,
        val sertifiseringer: List<SertifiseringValg>,
    ) {
        @Serializable
        data class SertifiseringValg(
            val id: Long,
            val navn: String,
        )
    }
}
