package no.nav.mulighetsrommet.arena

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ArenaMigrering {
    val TiltaksgjennomforingSluttDatoCutoffDate: LocalDate = LocalDate.of(2023, 1, 1)

    val EnkeltplassSluttDatoCutoffDate: LocalDate = LocalDate.of(2026, 1, 1)

    val ArenaTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    enum class Opphav {
        ARENA,
        TILTAKSADMINISTRASJON,
    }
}
