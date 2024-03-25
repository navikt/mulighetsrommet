package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

// TODO modellere om klasse til å passe bedre med domenet. Kalle det for en "ArrangorDto" i stedet?
@Serializable
data class VirksomhetDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val underenheter: List<VirksomhetDto>? = null,
    val postnummer: String?,
    val poststed: String?,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)

sealed class ArrangorDto {
    abstract val id: UUID
    abstract val organisasjonsnummer: String
    abstract val navn: String
    abstract val slettet: Boolean
}

@Serializable
data class ArrangorHovedenhet(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val organisasjonsnummer: String,
    override val navn: String,
    override val slettet: Boolean,
    val underenheter: List<ArrangorUnderenhet>,
    val kontaktperson: ArrangorKontaktperson?,
) : ArrangorDto()

@Serializable
data class ArrangorUnderenhet(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val organisasjonsnummer: String,
    override val navn: String,
    override val slettet: Boolean,
) : ArrangorDto()

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
)
