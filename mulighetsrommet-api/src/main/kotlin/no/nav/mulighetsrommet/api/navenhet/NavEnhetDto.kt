package no.nav.mulighetsrommet.api.navenhet

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer

// TODO: forenkle typer til å bedre represenere hvordan de benyttes i Tiltaksadministrasjon vs hvordan de er definert i NORG2
enum class NavEnhetType {
    FYLKE,
    LOKAL,
    TILTAK,
    ALS,
    KO,
    ARK,
}

@Serializable
data class NavEnhetDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val status: NavEnhetStatus,
    val type: NavEnhetType,
    val overordnetEnhet: NavEnhetNummer?,
)

@Serializable
data class NavRegionDto(
    val enhetsnummer: NavEnhetNummer,
    val navn: String,
    val status: NavEnhetStatus,
    val type: NavEnhetType,
    val enheter: List<NavEnhetDto>,
)
