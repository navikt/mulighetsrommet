package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.api.avtale.OpsjonLoggRequest
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.util.*

data class OpsjonLoggEntry(
    val avtaleId: UUID,
    val sluttdato: LocalDate?,
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonLoggRequest.OpsjonsLoggStatus,
    val registretDato: LocalDate,
    val registrertAv: NavIdent,
)
