package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

enum class ArrangorTil {
    AVTALE,
    TILTAKSGJENNOMFORING,
}

@Serializable
data class ArrangorDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val underenheter: List<ArrangorDto>? = null,
    val postnummer: String?,
    val poststed: String?,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)

@Serializable
data class ArrangorKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID,
    val navn: String,
    val beskrivelse: String?,
    val telefon: String?,
    val epost: String,
    val ansvarligFor: List<AnsvarligFor>? = emptyList(),
) {
    @Serializable
    enum class AnsvarligFor {
        AVTALE,
        TILTAKSGJENNOMFORING,
        OKONOMI,
    }
}
