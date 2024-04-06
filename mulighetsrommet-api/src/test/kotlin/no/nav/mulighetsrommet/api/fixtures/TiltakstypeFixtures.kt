package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TiltakstypeFixtures {
    val AFT = TiltakstypeDbo(
        id = UUID.fromString("59a64a02-efdd-471d-9529-356ff5553a5d"),
        navn = "Arbeidsforberedende trening (AFT)",
        arenaKode = "ARBFORB",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val VTA = TiltakstypeDbo(
        id = UUID.fromString("6fb921d6-0a87-4b8a-82a4-067477c1e113"),
        navn = "Varig tilrettelagt arbeid i skjermet virksomhet",
        arenaKode = "VASV",
        rettPaaTiltakspenger = false,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val GRUPPE_AMO = TiltakstypeDbo(
        id = UUID.fromString("6fb945d6-0a87-4b8a-82a4-067477c1e113"),
        navn = "Gruppe amo",
        arenaKode = "GRUPPEAMO",
        rettPaaTiltakspenger = false,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Oppfolging = TiltakstypeDbo(
        id = UUID.fromString("5b827950-cf47-4716-9305-bcf7f2646a00"),
        navn = "Oppfølging",
        arenaKode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Jobbklubb = TiltakstypeDbo(
        id = UUID.fromString("801340cd-f0ae-4da9-a01c-caad692933a2"),
        navn = "Jobbklubb",
        arenaKode = "JOBBK",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Avklaring = TiltakstypeDbo(
        id = UUID.fromString("75c4587a-4d99-4924-935b-4244abb81d32"),
        navn = "Avklaring",
        arenaKode = "AVKLARAG",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Arbeidstrening = TiltakstypeDbo(
        id = UUID.fromString("87cbc5c0-962e-4f34-93df-d78a887872a6"),
        navn = "Arbeidstrening",
        arenaKode = "ARBTREN",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2025, 12, 31),
    )

    val EnkelAmo = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Enkel AMO",
        arenaKode = "ENKELAMO",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2025, 12, 31),
    )
}
