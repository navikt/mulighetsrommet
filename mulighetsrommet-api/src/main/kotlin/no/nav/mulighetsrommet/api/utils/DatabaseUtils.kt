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

enum class DbStatus {
    AVLYST,
    PLANLAGT,
    AKTIV,
    AVSLUTTET,
    AVBRUTT;

    fun getFilterMedAvslutningsstatus(): String {
        return when (this) {
            AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            PLANLAGT -> "(${getFilter()} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AKTIV -> "(${getFilter()} and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AVSLUTTET -> "(${getFilter()} or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
        }
    }

    fun getFilter(startDatoNavn: String = "start_dato", sluttDatoNavn: String = "slutt_dato"): String {
        return when (this) {
            PLANLAGT -> "(:today < $startDatoNavn)"
            AKTIV -> "(:today >= $startDatoNavn and :today <= $sluttDatoNavn)"
            else -> "(:today > $sluttDatoNavn)"
        }
    }
}
