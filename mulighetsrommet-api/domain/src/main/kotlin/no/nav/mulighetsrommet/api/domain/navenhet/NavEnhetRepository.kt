package no.nav.mulighetsrommet.api.domain.navenhet

import no.nav.mulighetsrommet.model.NavEnhetNummer

interface NavEnhetRepository {
    fun save(navEnhet: NavEnhet)

    fun get(enhetsnummer: NavEnhetNummer): NavEnhet?

    fun getAll(
        statuser: List<NavEnhetStatus>? = null,
        typer: List<NavEnhetType>? = null,
        overordnetEnhet: NavEnhetNummer? = null,
    ): List<NavEnhet>

    fun deleteWhereEnhetsnummer(enhetsnummerForSletting: List<NavEnhetNummer>)
}
