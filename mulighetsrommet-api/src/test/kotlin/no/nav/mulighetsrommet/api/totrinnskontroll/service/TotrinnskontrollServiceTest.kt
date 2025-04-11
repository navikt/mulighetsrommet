package no.nav.mulighetsrommet.api.totrinnskontroll.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll.Type
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDateTime
import java.util.*

class TotrinnskontrollServiceTest : FunSpec({
    val navAnsattService = mockk<NavAnsattService>(relaxed = true)

    val service = TotrinnskontrollService(
        navAnsattService = navAnsattService,
    )

    every { navAnsattService.getNavAnsattByNavIdent(NavIdent("B123456")) } returns NavAnsatt(
        azureId = UUID.randomUUID(),
        navIdent = NavIdent("B123456"),
        fornavn = "Bertil",
        etternavn = "Bengtson",
        hovedenhet = NavAnsatt.Hovedenhet(
            enhetsnummer = NavEnhetNummer("1234"),
            navn = "Nav Enhet",
        ),
        mobilnummer = null,
        epost = "bertil.bengtson@nav.no",
        roller = emptySet(),
        skalSlettesDato = null,
    )

    context("TotrinnskontrollService") {
        test("Skal hente ut navn på beslutter") {
            val result = service.getBesluttetAvNavn(
                Totrinnskontroll(
                    id = UUID.randomUUID(),
                    entityId = UUID.randomUUID(),
                    type = Type.ANNULLER,
                    behandletAv = NavIdent("B123456"),
                    behandletTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                    besluttetAv = NavIdent("B123456"),
                    besluttetTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                ),
            )

            result shouldBe "Bertil Bengtson"
        }

        test("Skal hente ut navn på behandler") {
            val result = service.getBehandletAvNavn(
                Totrinnskontroll(
                    id = UUID.randomUUID(),
                    entityId = UUID.randomUUID(),
                    type = Type.ANNULLER,
                    behandletAv = NavIdent("B123456"),
                    behandletTidspunkt = LocalDateTime.now(),
                    aarsaker = emptyList(),
                    forklaring = null,
                    besluttetAv = NavIdent("B123456"),
                    besluttetTidspunkt = LocalDateTime.now(),
                    besluttelse = Besluttelse.GODKJENT,
                ),
            )

            result shouldBe "Bertil Bengtson"
        }
    }
})
