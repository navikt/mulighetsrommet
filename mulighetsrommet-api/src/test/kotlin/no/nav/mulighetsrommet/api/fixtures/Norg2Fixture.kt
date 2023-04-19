package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus

object Norg2EnhetFixture {
    val enhet = Norg2EnhetDto(
        enhetId = Math.random().toInt(),
        enhetNr = "1000",
        navn = "Enhet X",
        status = Norg2EnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
    )
}

object NavEnhetDboFixture {
    val enhetDbo = NavEnhetDbo(
        enhetId = Math.random().toInt(),
        enhetNr = "1000",
        navn = "Enhet X",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = "1200"
    )
}
