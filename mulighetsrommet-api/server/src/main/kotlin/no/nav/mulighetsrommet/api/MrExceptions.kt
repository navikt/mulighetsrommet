package no.nav.mulighetsrommet.api

import io.ktor.server.plugins.NotFoundException
import no.nav.mulighetsrommet.model.NavIdent

object MrExceptions {
    fun navAnsattNotFound(navIdent: NavIdent) = NotFoundException("Fant ikke ansatt med navIdent $navIdent")
}
