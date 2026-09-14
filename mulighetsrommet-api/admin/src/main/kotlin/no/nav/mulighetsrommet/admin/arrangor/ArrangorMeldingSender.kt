package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

/**
 * Port for å sende digitale meldinger med vedlegg til arrangør.
 */
interface ArrangorMeldingSender {
    suspend fun send(melding: TilsagnsbrevArrangorMelding): Either<MeldingError, MeldingId>
}

@Serializable
data class TilsagnsbrevArrangorMelding(
    val mottakerOrganisasjonsnummer: Organisasjonsnummer,
    val sendersReferanse: String,
    val innhold: Innhold,
    val varsel: Varsel,
    val vedlegg: Vedlegg,
    @Serializable(with = UUIDSerializer::class)
    val idempotentKey: UUID,
) {
    @Serializable
    data class Innhold(val tittel: String, val brevtekst: String)

    @Serializable
    data class Varsel(val emne: String, val tekst: String)

    @Serializable
    class Vedlegg(val navn: String, val innhold: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Vedlegg) return false
            return navn == other.navn && innhold.contentEquals(other.innhold)
        }

        override fun hashCode(): Int = 31 * navn.hashCode() + innhold.contentHashCode()
    }
}

data class MeldingId(val value: UUID)

data class MeldingError(val message: String)
