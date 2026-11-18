package no.nav.mulighetsrommet.api.tilsagn

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavIdent

sealed class TilsagnDtoQueryError {
    data class IkkeFunnet(val message: String) : TilsagnDtoQueryError()
    data class ManglerTilgang(val message: String, val navIdent: NavIdent, val roller: Set<Rolle>) : TilsagnDtoQueryError()
}
