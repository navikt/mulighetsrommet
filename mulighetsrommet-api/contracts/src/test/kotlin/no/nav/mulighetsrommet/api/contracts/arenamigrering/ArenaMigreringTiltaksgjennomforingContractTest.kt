package no.nav.mulighetsrommet.api.contracts.arenamigrering

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ArenaMigreringTiltaksgjennomforingContractTest : FunSpec({
    val gjennomforing = ArenaMigreringTiltaksgjennomforingDto(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        arenaId = 123456,
        tiltakskode = "GRUPPEAMO",
        startDato = LocalDate.of(2026, 3, 1),
        sluttDato = LocalDate.of(2026, 6, 30),
        opprettetTidspunkt = LocalDateTime.parse("2026-01-02T10:15:30"),
        endretTidspunkt = LocalDateTime.parse("2026-02-03T11:16:31.123456"),
        navn = "Norskopplæring",
        orgnummer = "999999999",
        antallPlasser = 15,
        status = ArenaTiltaksgjennomforingStatus.GJENNOMFORES,
        enhet = "0400",
        apentForInnsok = true,
        deltidsprosent = 80.5,
    )

    test("arenamigrering med alle felter") {
        assertContract(gjennomforing)
    }

    test("arenamigrering med nullable felter") {
        assertContract(gjennomforing.copy(arenaId = null, sluttDato = null, antallPlasser = null))
    }
})
