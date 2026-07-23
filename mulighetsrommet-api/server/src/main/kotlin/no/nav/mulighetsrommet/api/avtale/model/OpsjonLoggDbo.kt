package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonLoggStatus
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.util.UUID

data class OpsjonLoggDbo(
    val avtaleId: UUID,
    val sluttDato: LocalDate?,
    val forrigeSluttDato: LocalDate,
    val status: OpsjonLoggStatus,
    val registrertAv: NavIdent,
)

fun OpprettOpsjonLoggRequest.Type.toOpsjonLoggStatus() = when (this) {
    OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
    OpprettOpsjonLoggRequest.Type.ETT_AAR,
    -> OpsjonLoggStatus.OPSJON_UTLOST

    OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON
}
