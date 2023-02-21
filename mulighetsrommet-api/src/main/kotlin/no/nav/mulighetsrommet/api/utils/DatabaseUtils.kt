package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus

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

    fun getDbStatementMedAvslutningsstatus(): String {
        return when (this) {
            AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            PLANLAGT -> "(${getDbStatement()} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AKTIV -> "(${getDbStatement()} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AVSLUTTET -> "(${getDbStatement()} or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
        }
    }

    fun getDbStatement(startDatoNavn: String = "start_dato", sluttDatoNavn: String = "slutt_dato"): String {
        return when (this) {
            PLANLAGT -> "(:today < $startDatoNavn)"
            AKTIV -> "(:today >= $startDatoNavn and :today <= $sluttDatoNavn)"
            else -> "(:today > $sluttDatoNavn)"
        }
    }
}
