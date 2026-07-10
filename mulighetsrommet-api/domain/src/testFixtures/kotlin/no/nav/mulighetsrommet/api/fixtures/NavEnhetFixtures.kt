package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.model.NavEnhetNummer

object NavEnhetFixtures {
    val Innlandet = NavEnhet(
        navn = "Nav Innlandet",
        enhetsnummer = NavEnhetNummer("0400"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.FYLKE,
        overordnetEnhet = null,
    )
    val Gjovik = NavEnhet(
        navn = "Nav Gjøvik",
        enhetsnummer = NavEnhetNummer("0502"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )
    val Lillehammer = NavEnhet(
        navn = "Nav Lillehammer",
        enhetsnummer = NavEnhetNummer("0501"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )
    val Sel = NavEnhet(
        navn = "Nav Sel",
        enhetsnummer = NavEnhetNummer("0517"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = NavEnhetNummer("0400"),
    )

    val Oslo = NavEnhet(
        navn = "Nav Oslo",
        enhetsnummer = NavEnhetNummer("0300"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.FYLKE,
        overordnetEnhet = null,
    )
    val TiltakOslo = NavEnhet(
        navn = "Nav Tiltak Oslo",
        enhetsnummer = NavEnhetNummer("0387"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.TILTAK,
        overordnetEnhet = NavEnhetNummer("0300"),
    )
    val Sagene = NavEnhet(
        navn = "Nav Sagene",
        enhetsnummer = NavEnhetNummer("0314"),
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = NavEnhetNummer("0300"),
    )
}
