package no.nav.mulighetsrommet.brreg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
internal data class BrregEnhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val postAdresse: BrregAdresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class BrregUnderenhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val beliggenhetsadresse: BrregAdresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class BrregAdresse(
    val land: String? = null,
    val landkode: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val adresse: List<String>? = null,
    val kommune: String? = null,
    val kommunenummer: String? = null,
)

@Serializable
internal data class BrregEmbeddedHovedenheter(
    @Suppress("PropertyName")
    val _embedded: BrregHovedenheter? = null,
) {
    @Serializable
    internal data class BrregHovedenheter(
        val enheter: List<BrregUnderenhet>,
    )
}

@Serializable
internal data class BrregEmbeddedUnderenheter(
    @Suppress("PropertyName")
    val _embedded: BrregUnderenheter? = null,
) {
    @Serializable
    internal data class BrregUnderenheter(
        val underenheter: List<BrregUnderenhet>,
    )
}
