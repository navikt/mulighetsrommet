package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.util.*

object NavAnsattFixture {
    val ansatt1: NavAnsattDbo = NavAnsattDbo(
        navIdent = NavIdent("DD1"),
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "donald.duck@nav.no",
        roller = setOf(
            NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
            NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
            NavAnsattRolle.AVTALER_SKRIV,
        ),
        skalSlettesDato = null,
    )
    val ansatt2: NavAnsattDbo = NavAnsattDbo(
        navIdent = NavIdent("DD2"),
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "dolly.duck@nav.no",
        roller = setOf(
            NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
            NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
            NavAnsattRolle.AVTALER_SKRIV,
        ),
        skalSlettesDato = null,
    )
}
