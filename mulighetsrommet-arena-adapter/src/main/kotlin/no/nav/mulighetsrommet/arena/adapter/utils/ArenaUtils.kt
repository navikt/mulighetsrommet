package no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltakerStatus
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus
import java.time.LocalDateTime

object ArenaUtils {
    fun parseTimestamp(value: String): LocalDateTime {
        return LocalDateTime.parse(value, ArenaTimestampFormatter)
    }

    fun parseNullableTimestamp(value: String?): LocalDateTime? {
        return if (value != null && value != "null") {
            parseTimestamp(value)
        } else {
            null
        }
    }

    fun toDeltakerstatus(arenaStatus: ArenaTiltakdeltakerStatus): Deltakerstatus = when (arenaStatus) {
        ArenaTiltakdeltakerStatus.AVSLAG,
        ArenaTiltakdeltakerStatus.IKKE_AKTUELL,
        ArenaTiltakdeltakerStatus.TAKKET_NEI_TIL_TILBUD,
        -> Deltakerstatus.IKKE_AKTUELL

        ArenaTiltakdeltakerStatus.TILBUD,
        ArenaTiltakdeltakerStatus.TAKKET_JA_TIL_TILBUD,
        ArenaTiltakdeltakerStatus.INFORMASJONSMOTE,
        ArenaTiltakdeltakerStatus.AKTUELL,
        ArenaTiltakdeltakerStatus.VENTELISTE,
        -> Deltakerstatus.VENTER

        ArenaTiltakdeltakerStatus.GJENNOMFORES -> Deltakerstatus.DELTAR

        ArenaTiltakdeltakerStatus.DELTAKELSE_AVBRUTT,
        ArenaTiltakdeltakerStatus.GJENNOMFORING_AVBRUTT,
        ArenaTiltakdeltakerStatus.GJENNOMFORING_AVLYST,
        ArenaTiltakdeltakerStatus.IKKE_MOTT,
        ArenaTiltakdeltakerStatus.FULLFORT,
        -> Deltakerstatus.AVSLUTTET
    }

    fun parseJaNei(jaNeiStreng: JaNeiStatus): Boolean {
        return when (jaNeiStreng) {
            JaNeiStatus.Ja -> true
            JaNeiStatus.Nei -> false
        }
    }

    fun parseNulleableJaNei(jaNeiStreng: JaNeiStatus?): Boolean? {
        if (jaNeiStreng == null) {
            return null
        }
        return when (jaNeiStreng) {
            JaNeiStatus.Ja -> true
            JaNeiStatus.Nei -> false
        }
    }
}
