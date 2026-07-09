package no.nav.mulighetsrommet.admin.kostnadssted

import no.nav.mulighetsrommet.model.NavEnhetNummer

interface KostnadsstedQueryHandler {
    fun getAll(regioner: List<NavEnhetNummer> = listOf()): List<Kostnadssted>
}
