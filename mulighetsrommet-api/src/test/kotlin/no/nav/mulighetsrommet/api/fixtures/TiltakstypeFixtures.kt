package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.util.*

object TiltakstypeFixtures {
    val AFT = TiltakstypeDbo(
        id = UUID.fromString("59a64a02-efdd-471d-9529-356ff5553a5d"),
        navn = "Arbeidsforberedende trening (AFT)",
        arenaKode = "ARBFORB",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val VTA = TiltakstypeDbo(
        id = UUID.fromString("6fb921d6-0a87-4b8a-82a4-067477c1e113"),
        navn = "Varig tilrettelagt arbeid i skjermet virksomhet",
        arenaKode = "VASV",
        rettPaaTiltakspenger = false,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val ArbeidsrettetRehabilitering = TiltakstypeDbo(
        id = UUID.fromString("bc7128f9-3d5f-4190-a19a-ca392f17eb5c"),
        navn = "Arbeidsrettet rehabilitering",
        arenaKode = "ARBRRHDAG",
        rettPaaTiltakspenger = false,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val GruppeAmo = TiltakstypeDbo(
        id = UUID.fromString("ca0cbc97-0306-4d7d-a368-10087e71c365"),
        navn = "Gruppe amo",
        arenaKode = "GRUPPEAMO",
        rettPaaTiltakspenger = false,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val Oppfolging = TiltakstypeDbo(
        id = UUID.fromString("5b827950-cf47-4716-9305-bcf7f2646a00"),
        navn = "Oppfølging",
        arenaKode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val Jobbklubb = TiltakstypeDbo(
        id = UUID.fromString("801340cd-f0ae-4da9-a01c-caad692933a2"),
        navn = "Jobbklubb",
        arenaKode = "JOBBK",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val DigitalOppfolging = TiltakstypeDbo(
        id = UUID.fromString("54300eac-a537-418e-a28b-9fa984a8d36f"),
        navn = "Digital oppfølging",
        arenaKode = "DIGIOPPARB",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val Avklaring = TiltakstypeDbo(
        id = UUID.fromString("75c4587a-4d99-4924-935b-4244abb81d32"),
        navn = "Avklaring",
        arenaKode = "AVKLARAG",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
    )

    val GruppeFagOgYrkesopplaering = TiltakstypeDbo(
        id = UUID.fromString("bcee8523-70e1-4253-9000-6a1430ef4326"),
        navn = "Fag- og yrkesopplæring (Gruppe)",
        arenaKode = "GRUFAGYRKE",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = null,
    )

    val Arbeidstrening = TiltakstypeDbo(
        id = UUID.fromString("87cbc5c0-962e-4f34-93df-d78a887872a6"),
        navn = "Arbeidstrening",
        arenaKode = "ARBTREN",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = null,
    )

    val EnkelAmo = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Enkel AMO",
        arenaKode = "ENKELAMO",
        rettPaaTiltakspenger = true,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = null,
    )
}
