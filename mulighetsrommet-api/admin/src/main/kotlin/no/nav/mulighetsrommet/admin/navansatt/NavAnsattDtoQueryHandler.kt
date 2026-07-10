package no.nav.mulighetsrommet.admin.navansatt

import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavIdent

interface NavAnsattDtoQueryHandler {
    fun getAll(rollerContainsAll: List<NavAnsattRolle> = listOf()): List<NavAnsattDto>

    fun getByNavIdent(navIdent: NavIdent): NavAnsattDto?
}
