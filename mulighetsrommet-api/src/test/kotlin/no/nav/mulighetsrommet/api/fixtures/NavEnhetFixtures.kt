package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus

object NavEnhetFixtures {
    val IT = NavEnhetDbo(
        navn = "IT",
        enhetsnummer = "2990",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.DIR,
        overordnetEnhet = null,
    )

    val Innlandet = NavEnhetDbo(
        navn = "NAV Innlandet",
        enhetsnummer = "0400",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )
    val Gjovik = NavEnhetDbo(
        navn = "NAV Gjøvik",
        enhetsnummer = "0502",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = "0400",
    )

    val Oslo = NavEnhetDbo(
        navn = "NAV Oslo",
        enhetsnummer = "0300",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )
    val TiltakOslo = NavEnhetDbo(
        navn = "NAV Tiltak Oslo",
        enhetsnummer = "0387",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.TILTAK,
        overordnetEnhet = "0300",
    )
    val Sagene = NavEnhetDbo(
        navn = "NAV Sagene",
        enhetsnummer = "0314",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = "0300",
    )
}
