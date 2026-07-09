package no.nav.mulighetsrommet.admin.navenhet

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType

class KontorstrukturQuery(
    private val db: AdminDatabase,
) {
    fun execute(): List<Kontorstruktur> = db.session {
        val enheter = repository.navEnhet
            .getAll(
                statuser = listOf(
                    NavEnhetStatus.AKTIV,
                    NavEnhetStatus.UNDER_AVVIKLING,
                    NavEnhetStatus.UNDER_ETABLERING,
                ),
                typer = listOf(NavEnhetType.KO, NavEnhetType.LOKAL, NavEnhetType.FYLKE, NavEnhetType.ARK),
            )
            .filter {
                NavEnhetHelpers.erGeografiskEnhet(it.type) || NavEnhetHelpers.erSpesialenhetSomKanVelgesIModia(it.enhetsnummer)
            }
            .map { it.toDto() }

        Kontorstruktur.fromNavEnheter(enheter)
    }
}
