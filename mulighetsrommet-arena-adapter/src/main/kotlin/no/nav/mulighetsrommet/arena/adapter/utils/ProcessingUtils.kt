package no.nav.mulighetsrommet.arena.adapter.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.domain.Deltakerstatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ArenaEventOperationType(val type: String) {
    INSERT("I"),
    UPDATE("U");

    companion object {
        private val map = ArenaEventOperationType.values().associateBy(ArenaEventOperationType::type)
        operator fun get(value: String) = map[value]
    }
}

object ProcessingUtils {

    fun getArenaDateFormat(): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getArenaDateFromTo(value: String): LocalDateTime? {
        return if (value != "null") {
            LocalDateTime.parse(value, getArenaDateFormat())
        } else {
            null
        }
    }

    /**
     * 1 - Standard innsats
     * 2 - Situasjonsbestemt innsats
     * 3 - Spesiell tilpasset innsats
     * 4 - Varig tilpasset innsats
     * Inntil videre setter vi alle andre tiltakstyper vi ikke har kartlagt til standard innsats
     */
    fun toInnsatsgruppe(tiltakskode: String): Int = when (tiltakskode) {
        "JOBBK", "DIGIOPPARB" -> 1
        "AVKLARAG", "ARBTREN", "MIDLONTIL", "MENTOR", "INDOPPFAG", "INKLUTILS", "ENKFAGYRKE", "ENKELAMO" -> 2
        "HOYEREUTD", "ARBFORB" -> 3
        "VASV", "VATIAROR", "VARLONTIL" -> 4
        else -> 1
    }

    fun toDeltakerstatus(arenaStatus: String): Deltakerstatus = when (arenaStatus) {
        "AVSLAG", "IKKAKTUELL", "NEITAKK" -> Deltakerstatus.IKKE_AKTUELL
        "TILBUD", "JATAKK", "INFOMOETE", "AKTUELL", "VENTELISTE" -> Deltakerstatus.VENTER
        "GJENN" -> Deltakerstatus.DELTAR
        "DELAVB", "GJENN_AVB", "GJENN_AVL", "FULLF", "IKKEM" -> Deltakerstatus.AVSLUTTET
        else -> throw Exception("Ukjent deltakerstatus fra Arena")
    }
}
