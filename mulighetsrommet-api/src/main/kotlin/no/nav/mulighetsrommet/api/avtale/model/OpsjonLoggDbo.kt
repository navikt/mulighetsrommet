package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.util.*

data class OpsjonLoggDbo(
    val avtaleId: UUID,
    val sluttDato: LocalDate?,
    val forrigeSluttDato: LocalDate,
    val status: OpsjonLoggStatus,
    val registrertAv: NavIdent,
)

enum class OpsjonLoggStatus {
    OPSJON_UTLOST,
    SKAL_IKKE_UTLOSE_OPSJON,
    ;

    companion object {
        fun fromType(type: OpprettOpsjonLoggRequest.Type) = when (type) {
            OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            OpprettOpsjonLoggRequest.Type.ETT_AAR,
            -> OPSJON_UTLOST
            OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> SKAL_IKKE_UTLOSE_OPSJON
        }
    }
}
