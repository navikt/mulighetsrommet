package no.nav.mulighetsrommet.api.domain.testing

import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.model.NavEnhetNummer

class FakeNavEnhetRepository : NavEnhetRepository {
    private val store = mutableMapOf<NavEnhetNummer, NavEnhet>()

    override fun save(navEnhet: NavEnhet) {
        store[navEnhet.enhetsnummer] = navEnhet
    }

    override fun get(enhetsnummer: NavEnhetNummer): NavEnhet? {
        return store[enhetsnummer]
    }

    override fun getAll(
        statuser: List<NavEnhetStatus>?,
        typer: List<NavEnhetType>?,
        overordnetEnhet: NavEnhetNummer?,
    ): List<NavEnhet> {
        return store.values.filter { enhet ->
            (statuser == null || enhet.status in statuser) &&
                (typer == null || enhet.type in typer) &&
                (overordnetEnhet == null || enhet.overordnetEnhet == overordnetEnhet)
        }
    }
}
