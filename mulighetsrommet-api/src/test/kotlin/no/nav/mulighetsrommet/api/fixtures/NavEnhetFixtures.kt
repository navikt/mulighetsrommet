package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer

object NavEnhetFixtures {
    val Innlandet = NavEnhetDbo(
        navn = "Nav Innlandet",
        enhetsnummer = NavEnhetNummer("0400"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )
    val Gjovik = NavEnhetDbo(
        navn = "Nav Gjøvik",
        enhetsnummer = NavEnhetNummer("0502"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )
    val Lillehammer = NavEnhetDbo(
        navn = "Nav Lillehammer",
        enhetsnummer = NavEnhetNummer("0501"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )
    val Sel = NavEnhetDbo(
        navn = "Nav Sel",
        enhetsnummer = NavEnhetNummer("0517"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )

    val Oslo = NavEnhetDbo(
        navn = "Nav Oslo",
        enhetsnummer = NavEnhetNummer("0300"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )
    val TiltakOslo = NavEnhetDbo(
        navn = "Nav Tiltak Oslo",
        enhetsnummer = NavEnhetNummer("0387"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.TILTAK,
        overordnetEnhet = NavEnhetNummer("0300"),
    )
    val Sagene = NavEnhetDbo(
        navn = "Nav Sagene",
        enhetsnummer = NavEnhetNummer("0314"),
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = NavEnhetNummer("0300"),
    )
}
