package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

object Norg2EnhetFixture {
    val enhet = Norg2EnhetDto(
        enhetId = Math.random().toInt(),
        enhetNr = "1000",
        navn = "Enhet X",
        status = Norg2EnhetStatus.AKTIV,
        type = Norg2Type.LOKAL
    )
}
