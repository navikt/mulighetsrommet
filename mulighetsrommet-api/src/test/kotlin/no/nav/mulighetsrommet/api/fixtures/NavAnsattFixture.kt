package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import java.util.*

object NavAnsattFixture {
    val ansatt1: NavAnsattDbo = NavAnsattDbo(
        navIdent = "DD1",
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "donald.duck@nav.no",
        roller = setOf(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL, NavAnsattRolle.AVTALER_SKRIV),
    )
    val ansatt2: NavAnsattDbo = NavAnsattDbo(
        navIdent = "DD2",
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "dolly.duck@nav.no",
        roller = setOf(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL, NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
    )
}
