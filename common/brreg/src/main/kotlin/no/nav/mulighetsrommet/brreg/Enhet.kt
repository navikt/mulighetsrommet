package no.nav.mulighetsrommet.brreg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
internal data class Enhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val postAdresse: Adresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class Underenhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val beliggenhetsadresse: Adresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class Adresse(
    val land: String? = null,
    val landkode: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val adresse: List<String>? = null,
    val kommune: String? = null,
    val kommunenummer: String? = null,
)

@Serializable
internal data class EmbeddedEnheter(
    @Suppress("PropertyName")
    val _embedded: Enheter? = null,
) {
    @Serializable
    internal data class Enheter(
        val enheter: List<Enhet>,
    )
}

@Serializable
internal data class EmbeddedUnderenheter(
    @Suppress("PropertyName")
    val _embedded: Underenheter? = null,
) {
    @Serializable
    internal data class Underenheter(
        val underenheter: List<Underenhet>,
    )
}
