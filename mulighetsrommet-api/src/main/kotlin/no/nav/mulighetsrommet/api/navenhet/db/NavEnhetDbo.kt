package no.nav.mulighetsrommet.api.navenhet.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class NavEnhetDbo(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val status: NavEnhetStatus,
    val type: Norg2Type,
    val overordnetEnhet: NavEnhetNummer? = null,
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT,
}

/**
 * Det finnes mange aktive Nav-enheter i Arena med [enhetsnummer] som ikke er reflektert i NORG.
 * Derfor støtter vi at enheten er definert, men at [navn] er nullable.
 *
 * Man må også være oppmerksom på at det finnes noen Nav-enheter i både Arena og NORG som har samme [enhetsnummer],
 * men forskjellig [navn] - altså at Nav-enhtene kan ha forskjellig betydning ifm. økonomi i de to systemene.
 * Dette får vi ikke gjort så mye med i vår løsning, dermed viser vi alltid [navn] vi får fra NORG.
 */
@Serializable
data class ArenaNavEnhet(
    val navn: String?,
    val enhetsnummer: String,
)
