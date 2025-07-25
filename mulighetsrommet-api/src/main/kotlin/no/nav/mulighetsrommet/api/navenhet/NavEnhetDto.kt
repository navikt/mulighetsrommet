package no.nav.mulighetsrommet.api.navenhet

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.model.NavEnhetNummer

// TODO: forenkle typer til å bedre represenere hvordan de benyttes i Tiltaksadministrasjon vs hvordan de er definert i NORG2
enum class NavEnhetType {
    KO,
    FYLKE,
    TILTAK,
    AAREG,
    ALS,
    ARK,
    DIR,
    DOKSENTER,
    EKSTERN,
    FORVALTNING,
    FPY,
    HELFO,
    HMS,
    INNKREV,
    INTRO,
    IT,
    KLAGE,
    KONTAKT,
    KONTROLL,
    LOKAL,
    OKONOMI,
    OTENESTE,
    OPPFUTLAND,
    OTENESE,
    RIKSREV,
    ROBOT,
    ROL,
    TILLIT,
    UTLAND,
    YTA,
}

@Serializable
data class NavEnhetDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val type: NavEnhetType,
    val overordnetEnhet: NavEnhetNummer?,
)

@Serializable
data class NavRegionDto(
    val enhetsnummer: NavEnhetNummer,
    val navn: String,
    val enheter: List<NavRegionUnderenhetDto>,
)

@Serializable
data class NavRegionUnderenhetDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val overordnetEnhet: NavEnhetNummer,
    val erStandardvalg: Boolean,
)
