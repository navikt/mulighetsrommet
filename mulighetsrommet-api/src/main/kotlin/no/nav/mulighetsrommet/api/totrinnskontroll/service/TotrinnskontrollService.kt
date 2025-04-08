package no.nav.mulighetsrommet.api.totrinnskontroll.service

import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent

class TotrinnskontrollService(private val navAnsattService: NavAnsattService) {
    fun getBesluttetAvNavn(totrinnskontroll: Totrinnskontroll): String? {
        return if (totrinnskontroll.besluttelse != null && totrinnskontroll.besluttetAv is NavIdent) {
            val beslutter = navAnsattService.getNavAnsattByNavIdent(totrinnskontroll.besluttetAv)
            beslutter?.let { "${beslutter.fornavn} ${beslutter.etternavn}" }
        } else {
            null
        }
    }

    fun getBehandletAvNavn(totrinnskontroll: Totrinnskontroll): String? {
        return if (totrinnskontroll.besluttelse != null && totrinnskontroll.behandletAv is NavIdent) {
            val beslutter = navAnsattService.getNavAnsattByNavIdent(totrinnskontroll.behandletAv)
            beslutter?.let { "${beslutter.fornavn} ${beslutter.etternavn}" }
        } else {
            null
        }
    }
}
