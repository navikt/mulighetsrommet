package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TiltakstypeFixtures {
    val Oppfolging = TiltakstypeDbo(
        id = UUID.fromString("5b827950-cf47-4716-9305-bcf7f2646a00"),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        fraDato = LocalDate.of(2023, 1, 1),
        tilDato = LocalDate.of(2023, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Arbeidstrening = TiltakstypeDbo(
        id = UUID.fromString("87cbc5c0-962e-4f34-93df-d78a887872a6"),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )
}
