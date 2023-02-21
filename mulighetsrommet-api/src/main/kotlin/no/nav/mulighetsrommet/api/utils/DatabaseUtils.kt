package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import java.time.LocalDate

object DatabaseUtils {
    fun andWhereParameterNotNull(vararg parts: Pair<Any?, String?>): String = parts
        .filter { it.first != null }
        .map { it.second }
        .reduceOrNull { where, part -> "$where and $part" }
        ?.let { "where $it" }
        ?: ""
}

enum class StatusDbStatement {
    AVLYST,
    PLANLAGT,
    AKTIV,
    AVSLUTTET,
    AVBRUTT;

    fun getDbStatementMedAvslutningsstatus(dagensDato: LocalDate): String {
        return when (this) {
            AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            PLANLAGT -> "(${getDbStatement(dagensDato)} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AKTIV -> "(${getDbStatement(dagensDato)} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AVSLUTTET -> "(${getDbStatement(dagensDato)} or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
        }
    }

    fun getDbStatement(dagensDato: LocalDate, startDatoNavn: String = "start_dato", sluttDatoNavn: String = "slutt_dato"): String {
        return when (this) {
            PLANLAGT -> "('$dagensDato' < $startDatoNavn)"
            AKTIV -> "('$dagensDato' >= $startDatoNavn and '$dagensDato' <= $sluttDatoNavn)"
            else -> "('$dagensDato' > $sluttDatoNavn)"
        }
    }
}
