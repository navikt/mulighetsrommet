package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

@Serializable
data class NavEnhetDbo(
    val navn: String,
    val enhetsnummer: String,
    val status: NavEnhetStatus,
    val type: Norg2Type,
    val overordnetEnhet: String? = null,
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT,
}

/**
 * Det finnes mange aktive NAV-enheter i Arena med [enhetsnummer] som ikke er reflektert i NORG.
 * Derfor støtter vi at enheten er definert, men at [navn] er nullable.
 *
 * Man må også være oppmerksom på at det finnes noen NAV-enheter i både Arena og NORG som har samme [enhetsnummer],
 * men forskjellig [navn] - altså at NAV-enhtene kan ha forskjellig betydning ifm. økonomi i de to systemene.
 * Dette får vi ikke gjort så mye med i vår løsning, dermed viser vi alltid [navn] vi får fra NORG.
 */
@Serializable
data class ArenaNavEnhet(
    val navn: String?,
    val enhetsnummer: String,
)
