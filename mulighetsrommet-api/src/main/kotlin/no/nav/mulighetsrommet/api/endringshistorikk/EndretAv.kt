package no.nav.mulighetsrommet.api.endringshistorikk

import no.nav.mulighetsrommet.model.NavIdent

const val ENDRINGSHISTORIKK_TILTAKSADMINISTRASJON_SYSTEM_BRUKER = "System"

const val ENDRINGSHISTORIKK_ARENA_SYSTEM_BRUKER = "Arena"

const val ENDRINGSHISTORIKK_ARRANGOR = "Arrangør"

sealed class EndretAv(val id: String) {
    class NavAnsatt(navIdent: NavIdent) : EndretAv(navIdent.value)

    data object Arrangor : EndretAv(ENDRINGSHISTORIKK_ARRANGOR)

    data object System : EndretAv(ENDRINGSHISTORIKK_TILTAKSADMINISTRASJON_SYSTEM_BRUKER)

    data object Arena : EndretAv(ENDRINGSHISTORIKK_ARENA_SYSTEM_BRUKER)
}
