package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.util.*

data class OpsjonLoggEntry(
    val avtaleId: UUID,
    val sluttdato: LocalDate?,
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonLoggStatus,
    val registretDato: LocalDate,
    val registrertAv: NavIdent,
)

enum class OpsjonLoggStatus {
    OPSJON_UTLOST,
    SKAL_IKKE_UTLOSE_OPSJON,
}
