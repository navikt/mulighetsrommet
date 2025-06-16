package no.nav.mulighetsrommet.arena

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ArenaMigrering {
    val TiltaksgjennomforingSluttDatoCutoffDate: LocalDate = LocalDate.of(2023, 1, 1)
    val ArenaAvtaleCutoffDateTime: LocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0)
    val ArenaTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    enum class Opphav {
        ARENA,
        TILTAKSADMINISTRASJON,
    }
}
