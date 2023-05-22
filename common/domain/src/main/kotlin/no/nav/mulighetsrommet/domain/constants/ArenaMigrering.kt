package no.nav.mulighetsrommet.domain.constants

import java.time.LocalDate
import java.time.LocalDateTime

object ArenaMigrering {
    val TiltaksgjennomforingSluttDatoCutoffDate = LocalDate.of(2023, 1, 1)
    val ArenaAvtaleCutoffDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0)
}
