package no.nav.mulighetsrommet.domain.constants

import java.time.LocalDate
import java.time.LocalDateTime

object ArenaMigrering {
    val TiltaksgjennomforingSluttDatoCutoffDate: LocalDate = LocalDate.of(2023, 1, 1)
    val ArenaAvtaleCutoffDateTime: LocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0)

    enum class Opphav {
        ARENA,
        MR_ADMIN_FLATE,
    }
}
