package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.*

object NavAnsattFixture {
    val DonaldDuck: NavAnsattDbo = NavAnsattDbo(
        navIdent = NavIdent("DD1"),
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "donald.duck@nav.no",
        skalSlettesDato = null,
    )
    val MikkeMus: NavAnsattDbo = NavAnsattDbo(
        navIdent = NavIdent("DD2"),
        fornavn = "Mikke",
        etternavn = "Mus",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "mikke.mus@nav.no",
        skalSlettesDato = null,
    )
    val FetterAnton: NavAnsattDbo = NavAnsattDbo(
        navIdent = NavIdent("DD3"),
        fornavn = "Fetter",
        etternavn = "Anton",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "fetter.anton@nav.no",
        skalSlettesDato = null,
    )
}
