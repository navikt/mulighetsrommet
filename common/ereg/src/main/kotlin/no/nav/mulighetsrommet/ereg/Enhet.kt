package no.nav.mulighetsrommet.ereg

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

/**
 * Ereg sin `Organisasjon`-modell er av typene [Virksomhet], [JuridiskEnhet] og [Organisasjonsledd].
 */
@Serializable
internal sealed class Organisasjon {
    abstract val organisasjonsnummer: Organisasjonsnummer
    abstract val navn: Navn?
    abstract val organisasjonDetaljer: OrganisasjonDetaljer?
}

@Serializable
@SerialName("Virksomhet")
internal data class Virksomhet(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: Navn? = null,
    override val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val bestaarAvOrganisasjonsledd: List<BestaarAvOrganisasjonsledd> = emptyList(),
    val inngaarIJuridiskEnheter: List<JuridiskEnhetKnytning> = emptyList(),
) : Organisasjon()

@Serializable
@SerialName("JuridiskEnhet")
internal data class JuridiskEnhet(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: Navn? = null,
    override val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val driverVirksomheter: List<DriverVirksomhet> = emptyList(),
) : Organisasjon()

@Serializable
@SerialName("Organisasjonsledd")
internal data class Organisasjonsledd(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: Navn? = null,
    override val organisasjonDetaljer: OrganisasjonDetaljer? = null,
    val driverVirksomheter: List<DriverVirksomhet> = emptyList(),
    val inngaarIJuridiskEnheter: List<JuridiskEnhetKnytning> = emptyList(),
    val organisasjonsleddOver: List<BestaarAvOrganisasjonsledd> = emptyList(),
) : Organisasjon()

/**
 * Ereg nøster hele organisasjonsledd-kjeden i ett og samme svar når `inkluderHierarki=true`
 * er satt, i stedet for at man må gjøre ett HTTP-kall per nivå i kjeden.
 */
@Serializable
internal data class BestaarAvOrganisasjonsledd(
    val organisasjonsledd: Organisasjonsledd? = null,
)

/**
 * Følger kjeden av organisasjonsledd helt til toppen, og returnerer orgnr til den juridiske enheten
 * kjeden til slutt inngår i.
 */
internal fun Virksomhet.finnJuridiskEnhet(): Organisasjonsnummer? {
    val direkte = inngaarIJuridiskEnheter.firstNotNullOfOrNull { it.organisasjonsnummer }
    if (direkte != null) {
        return direkte
    }

    return bestaarAvOrganisasjonsledd.firstNotNullOfOrNull { it.organisasjonsledd }?.finnJuridiskEnhet()
}

internal fun Organisasjonsledd.finnJuridiskEnhet(): Organisasjonsnummer? {
    val direkte = inngaarIJuridiskEnheter.firstNotNullOfOrNull { it.organisasjonsnummer }
    if (direkte != null) {
        return direkte
    }

    return organisasjonsleddOver.firstNotNullOfOrNull { it.organisasjonsledd }?.finnJuridiskEnhet()
}

/**
 * Returnerer orgnr til enheten rett over i hierarkiet (ett hopp), enten det er en juridisk enhet
 * organisasjonsleddet inngår direkte i, eller et overliggende organisasjonsledd. Tilsvarer semantikken
 * til `overordnetEnhet` i Brreg, der man selv må følge kjeden videre for å nå toppen.
 */
internal fun Organisasjonsledd.finnDirekteOverordnetEnhet(): Organisasjonsnummer? {
    return inngaarIJuridiskEnheter.firstNotNullOfOrNull { it.organisasjonsnummer }
        ?: organisasjonsleddOver.firstNotNullOfOrNull { it.organisasjonsledd?.organisasjonsnummer }
}

@Serializable
internal data class Navn(
    val sammensattnavn: String? = null,
    val navnelinje1: String? = null,
)

internal fun Navn?.tilNavnString(orgnr: Organisasjonsnummer): String = this?.sammensattnavn ?: this?.navnelinje1 ?: orgnr.value

@Serializable
internal data class OrganisasjonDetaljer(
    /**
     * Ereg sitt api skiller ikke eksplisitt mellom "slettet" og "fjernet av juridiske årsaker" slik Brreg gjør
     * (410 Gone). Vi bruker [opphoersdato] som eneste signal på at en enhet ikke lenger er aktiv.
     */
    @Serializable(with = LocalDateSerializer::class)
    val opphoersdato: LocalDate? = null,
    val enhetstyper: List<Enhetstype> = emptyList(),
    val forretningsadresser: List<Adresse> = emptyList(),
    val postadresser: List<Adresse> = emptyList(),
)

@Serializable
internal data class Enhetstype(
    val enhetstype: String? = null,
)

internal fun List<Enhetstype>.tilOrganisasjonsform(): String? = lastOrNull()?.enhetstype

@Serializable
internal data class Adresse(
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
) {
    fun toEregAdresse(): EregAdresse = EregAdresse(
        landkode = landkode,
        postnummer = postnummer,
        poststed = poststed,
        adresse = listOfNotNull(adresselinje1, adresselinje2, adresselinje3),
    )
}

@Serializable
internal data class JuridiskEnhetKnytning(
    val organisasjonsnummer: Organisasjonsnummer? = null,
)

@Serializable
internal data class DriverVirksomhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: Navn? = null,
)

/**
 * Wire-modell for søkeresultater fra Ereg sitt `GET /v2/organisasjon/finn`-endepunkt. Dette gir et
 * lettvekts sammendrag av treff, uten f.eks. full postadresse - i motsetning til `GET /v2/organisasjon/{orgnr}`.
 */
@Serializable
internal data class OrganisasjonSammendragResultat(
    val organisasjonSammendrag: List<OrganisasjonSammendrag> = emptyList(),
)

@Serializable
internal data class OrganisasjonSammendrag(
    val organisasjonsnummer: Organisasjonsnummer,
    /**
     * Kodeverk for enhetstype/organisasjonsform (se https://www.brreg.no/bedrift/organisasjonsformer/),
     * f.eks. "BEDR"/"AAFY" for underenheter, "ORGL" for organisasjonsledd, eller en organisasjonsform
     * som "AS"/"ENK" for juridiske enheter.
     */
    val enhetstype: String? = null,
    val sammensattnavn: String? = null,
    val navnelinje1: String? = null,
    val juridiskEnhetOrganisasjonsnummer: Organisasjonsnummer? = null,
    val adresselinje1: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    val opphoert: Boolean? = null,
) {
    fun tilNavnString(): String = sammensattnavn ?: navnelinje1 ?: organisasjonsnummer.value

    /**
     * Det finnes to organisasjonsformer for underenheter i Brreg/Ereg sitt kodeverk: "BEDR"
     * (underenhet til næringsdrivende og offentlig forvaltning) og "AAFY" (underenhet til
     * ikke-næringsdrivende).
     */
    fun erVirksomhet(): Boolean = enhetstype in ENHETSTYPE_KODER_UNDERENHET

    fun tilEregAdresse(): EregAdresse? = if (adresselinje1 == null && postnummer == null && poststed == null) {
        null
    } else {
        EregAdresse(
            landkode = landkode,
            postnummer = postnummer,
            poststed = poststed,
            adresse = listOfNotNull(adresselinje1),
        )
    }
}

private val ENHETSTYPE_KODER_UNDERENHET = setOf("BEDR", "AAFY")
