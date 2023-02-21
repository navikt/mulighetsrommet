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
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT;

    fun getDbStatement(dagensDato: LocalDate): String {
        return when (this) {
            AVLYST -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            PLANLAGT -> "('$dagensDato' < start_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            GJENNOMFORES -> "('$dagensDato' >= start_dato and '$dagensDato' <= slutt_dato and avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}')"
            AVSLUTTET -> "('$dagensDato' > slutt_dato or avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}')"
            AVBRUTT -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
        }
    }
}
