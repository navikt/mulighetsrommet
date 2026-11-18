package no.nav.mulighetsrommet.api.contracts.tiltakstype

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import java.time.LocalDateTime
import java.util.UUID

class TiltakstypeV3ContractTest : FunSpec({
    val tiltakstype = TiltakstypeV3Dto(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        navn = "Arbeidsmarkedsopplæring",
        tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        system = TiltakstypeSystem.TILTAKSADMINISTRASJON,
        innsatsgrupper = setOf(Innsatsgruppe.TRENGER_VEILEDNING, Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE),
        arenaKode = "GRUPPEAMO",
        deltakerRegistreringInnhold = DeltakerRegistreringInnholdDto(
            innholdselementer = listOf(Innholdselement(tekst = "Norskopplæring", innholdskode = "norskopplaering")),
            ledetekst = "Velg innhold",
        ),
        opprettetTidspunkt = LocalDateTime.parse("2026-01-02T10:15:30"),
        oppdatertTidspunkt = LocalDateTime.parse("2026-02-03T11:16:31.123456"),
    )

    test("tiltakstype med alle felter") {
        assertContract(tiltakstype)
    }

    test("tiltakstype med nullable felter og tomme innsatsgrupper") {
        assertContract(tiltakstype.copy(arenaKode = null, deltakerRegistreringInnhold = null, innsatsgrupper = emptySet()))
    }
})
