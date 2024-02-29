package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.Gruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TiltakstypeFixtures {
    val AFT = TiltakstypeDbo(
        id = UUID.fromString("59a64a02-efdd-471d-9529-356ff5553a5d"),
        navn = "Arbeidsforberedende trening (AFT)",
        tiltakskode = Gruppetiltak.ARBEIDSFORBEREDENDE_TRENING,
        arenaKode = "ARBFORB",
        rettPaaTiltakspenger = true,
        fraDato = LocalDate.of(2023, 1, 1),
        tilDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val VTA = TiltakstypeDbo(
        id = UUID.fromString("6fb921d6-0a87-4b8a-82a4-067477c1e113"),
        navn = "Varig tilrettelagt arbeid i skjermet virksomhet",
        tiltakskode = Gruppetiltak.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        arenaKode = "VASV",
        rettPaaTiltakspenger = false,
        fraDato = LocalDate.of(2023, 1, 1),
        tilDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Oppfolging = TiltakstypeDbo(
        id = UUID.fromString("5b827950-cf47-4716-9305-bcf7f2646a00"),
        navn = "Oppfølging",
        tiltakskode = Gruppetiltak.OPPFOLGING,
        arenaKode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        fraDato = LocalDate.of(2023, 1, 1),
        tilDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Jobbklubb = TiltakstypeDbo(
        id = UUID.fromString("801340cd-f0ae-4da9-a01c-caad692933a2"),
        navn = "Jobbklubb",
        tiltakskode = Gruppetiltak.JOBBKLUBB,
        arenaKode = "JOBBK",
        rettPaaTiltakspenger = true,
        fraDato = LocalDate.of(2023, 1, 1),
        tilDato = LocalDate.of(2025, 12, 31),
        registrertDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Arbeidstrening = TiltakstypeDbo(
        id = UUID.fromString("87cbc5c0-962e-4f34-93df-d78a887872a6"),
        navn = "Arbeidstrening",
        tiltakskode = Gruppetiltak.ARBEIDSFORBEREDENDE_TRENING,
        arenaKode = "ARBTREN",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2025, 12, 31),
    )

    val EnkelAmo = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Enkel AMO",
        tiltakskode = null,
        arenaKode = "ENKELAMO",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2025, 12, 31),
    )
}
