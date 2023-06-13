package no.nav.mulighetsrommet.api.domain.dbo

import java.util.*

data class NavAnsattDbo(
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    /**
     * Enhetsnummer til den ansattes hovedenhet.
     */
    val hovedenhet: String,
    val azureId: UUID,
    val fraAdGruppe: UUID,
    val mobilnummer: String? = null,
    val epost: String,
)
