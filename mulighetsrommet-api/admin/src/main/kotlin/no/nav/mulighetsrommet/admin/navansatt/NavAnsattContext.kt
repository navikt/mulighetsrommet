package no.nav.mulighetsrommet.admin.navansatt

import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class NavAnsattContext(
    val navIdent: NavIdent,
    val accessType: AccessType.OBO.AzureAd,
)
