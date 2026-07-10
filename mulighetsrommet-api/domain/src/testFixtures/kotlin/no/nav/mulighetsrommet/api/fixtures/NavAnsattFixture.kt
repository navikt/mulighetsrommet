package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

object NavAnsattFixture {
    val DonaldDuck: NavAnsatt = NavAnsatt.opprett(
        navIdent = NavIdent("DD1"),
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "donald.duck@nav.no",
        roller = setOf(),
    )
    val MikkeMus: NavAnsatt = NavAnsatt.opprett(
        navIdent = NavIdent("DD2"),
        fornavn = "Mikke",
        etternavn = "Mus",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "mikke.mus@nav.no",
        roller = setOf(),
    )
    val FetterAnton: NavAnsatt = NavAnsatt.opprett(
        navIdent = NavIdent("DD3"),
        fornavn = "Fetter",
        etternavn = "Anton",
        hovedenhet = NavEnhetNummer("0400"),
        entraObjectId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "fetter.anton@nav.no",
        roller = setOf(),
    )
}
