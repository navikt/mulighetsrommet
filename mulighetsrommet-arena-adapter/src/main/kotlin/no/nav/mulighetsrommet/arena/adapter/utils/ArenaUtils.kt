package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ArenaUtils {
    val TimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun isGruppetiltak(tiltakskode: String): Boolean {
        // Enn så lenge så opererer vi med en hardkodet liste over hvilke gjennomføringer vi anser som gruppetiltak
        val gruppetiltak = listOf(
            "ARBFORB",
            "ARBRRHDAG",
            "AVKLARAG",
            "DIGIOPPARB",
            "FORSAMOGRU",
            "FORSFAGGRU",
            "GRUFAGYRKE",
            "GRUPPEAMO",
            "INDJOBSTOT",
            "INDOPPFAG",
            "INDOPPRF",
            "IPSUNG",
            "JOBBK",
            "UTVAOONAV",
            "UTVOPPFOPL",
            "VASV",
        )
        return tiltakskode in gruppetiltak
    }

    fun parseTimestamp(value: String): LocalDateTime {
        return LocalDateTime.parse(value, TimestampFormatter)
    }

    fun parseNullableTimestamp(value: String?): LocalDateTime? {
        return if (value != null && value != "null") {
            parseTimestamp(value)
        } else {
            null
        }
    }

    fun toDeltakerstatus(arenaStatus: String): Deltakerstatus = when (arenaStatus) {
        "AVSLAG", "IKKAKTUELL", "NEITAKK" -> Deltakerstatus.IKKE_AKTUELL
        "TILBUD", "JATAKK", "INFOMOETE", "AKTUELL", "VENTELISTE" -> Deltakerstatus.VENTER
        "GJENN" -> Deltakerstatus.DELTAR
        "DELAVB", "GJENN_AVB", "GJENN_AVL", "FULLF", "IKKEM" -> Deltakerstatus.AVSLUTTET
        else -> throw Exception("Ukjent deltakerstatus fra Arena")
    }
}
