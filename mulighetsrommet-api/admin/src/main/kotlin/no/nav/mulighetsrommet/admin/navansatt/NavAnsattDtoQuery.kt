package no.nav.mulighetsrommet.admin.navansatt

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavIdent

class NavAnsattDtoQuery(
    private val db: AdminDatabase,
) {
    fun getAll(rollerContainsAll: List<NavAnsattRolle> = listOf()): List<NavAnsattDto> = db.session {
        queries.navAnsattDto.getAll(rollerContainsAll)
    }

    fun get(navIdent: NavIdent): NavAnsattDto? = db.session {
        queries.navAnsattDto.getByNavIdent(navIdent)
    }
}
