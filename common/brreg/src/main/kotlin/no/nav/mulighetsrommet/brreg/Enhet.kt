package no.nav.mulighetsrommet.brreg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
internal data class OverordnetEnhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val organisasjonsform: Organisasjonsform,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val postadresse: Adresse? = null,
    val forretningsadresse: Adresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class Underenhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val organisasjonsform: Organisasjonsform,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val beliggenhetsadresse: Adresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
)

@Serializable
internal data class Organisasjonsform(
    val kode: String,
    @Serializable(with = LocalDateSerializer::class)
    val utgaatt: LocalDate? = null,
    val beskrivelse: String,
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
) {
    fun toBrregAdresse(): BrregAdresse = BrregAdresse(
        landkode = landkode,
        postnummer = postnummer,
        poststed = poststed,
        adresse = adresse,
    )
}

@Serializable
internal data class EmbeddedEnheter(
    @Suppress("PropertyName")
    val _embedded: Enheter? = null,
) {
    @Serializable
    internal data class Enheter(
        val enheter: List<OverordnetEnhet>,
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
