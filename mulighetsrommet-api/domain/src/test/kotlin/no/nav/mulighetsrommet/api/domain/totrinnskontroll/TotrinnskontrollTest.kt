package no.nav.mulighetsrommet.api.domain.totrinnskontroll

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import java.util.UUID

class TotrinnskontrollTest : FunSpec({
    val behandletAv = NavIdent("DD1")
    val besluttetAv = NavIdent("DD2")
    val entityId: UUID = UUID.randomUUID()

    fun opprett(
        type: TotrinnskontrollType = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Totrinnskontroll = Totrinnskontroll.opprett(UUID.randomUUID(), entityId, type, behandletAv, aarsaker, forklaring)

    context("opprett") {
        test("oppretter TIL_BEHANDLING med riktig behandletAv") {
            val opprettelse = opprett(TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            opprettelse.entityId shouldBe entityId
            opprettelse.behandletAv shouldBe behandletAv
            opprettelse.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
            opprettelse.besluttetAv shouldBe null
        }

        test("støtter årsaker og forklaring") {
            val opprettelse = opprett(
                TotrinnskontrollType.TILSAGN_ANNULLERING,
                listOf("FEIL_PERIODE"),
                "Perioden er feil",
            )
            opprettelse.aarsaker shouldBe listOf("FEIL_PERIODE")
            opprettelse.forklaring shouldBe "Perioden er feil"
        }
    }

    context("godkjenn") {
        test("godkjenner og returnerer oppdatert tilstand") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.status shouldBe TotrinnskontrollStatus.GODKJENT
            godkjent.besluttetAv shouldBe besluttetAv
            godkjent.besluttetTidspunkt shouldNotBe null
        }

        test("feiler når behandletAv og besluttetAv er samme NavIdent") {
            opprett().godkjenn(behandletAv) shouldBeLeft TotrinnskontrollError.KanIkkeBesluttesAvBehandler
        }

        test("feiler når allerede godkjent") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.godkjenn(besluttetAv) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(TotrinnskontrollStatus.GODKJENT)
        }

        test("feiler når allerede returnert") {
            val returnert = opprett().returner(besluttetAv).shouldBeRight()
            returnert.godkjenn(besluttetAv) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(TotrinnskontrollStatus.RETURNERT)
        }

        test("godkjenning uten årsaker-override beholder eksisterende årsaker") {
            val opprettelse = opprett(
                TotrinnskontrollType.TILSAGN_ANNULLERING,
                listOf("FEIL_PERIODE"),
            )
            val godkjent = opprettelse.godkjenn(besluttetAv).shouldBeRight()
            godkjent.aarsaker shouldBe listOf("FEIL_PERIODE")
        }
    }

    context("returner") {
        test("returnerer og oppdaterer tilstand") {
            val returnert = opprett().returner(besluttetAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            returnert.status shouldBe TotrinnskontrollStatus.RETURNERT
            returnert.besluttetAv shouldBe besluttetAv
            returnert.besluttetTidspunkt shouldNotBe null
            returnert.aarsaker shouldBe listOf("FEIL_BELOP")
            returnert.forklaring shouldBe "Beløp er feil"
        }

        test("retur kan gjøres av samme NavIdent som behandletAv") {
            val returnert = opprett().returner(behandletAv, listOf("FEIL_BELOP"), "Beløp er feil").shouldBeRight()
            returnert.status shouldBe TotrinnskontrollStatus.RETURNERT
            returnert.besluttetAv shouldBe behandletAv
        }

        test("feiler når allerede godkjent og besluttetAv er NavIdent") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.returner(besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(
                TotrinnskontrollStatus.GODKJENT,
            )
        }

        test("feiler når allerede returnert") {
            val returnert = opprett().returner(besluttetAv).shouldBeRight()
            returnert.returner(besluttetAv, listOf("FEIL_BELOP")) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(
                TotrinnskontrollStatus.RETURNERT,
            )
        }

        test("systemet er tillatt å endre fra godkjent til returnert") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.returner(Tiltaksadministrasjon, listOf("PROPAGERT_RETUR")).shouldBeRight()
        }
    }

    context("settPaVent") {
        test("setter på vent") {
            val paVent = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                .settPaVent(besluttetAv, forklaring = "Trenger mer info")
                .shouldBeRight()
            paVent.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
            paVent.besluttetAv shouldBe besluttetAv
        }
    }

    context("tilbakestill") {
        fun sattPaVent(): Totrinnskontroll = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI)
            .settPaVent(besluttetAv, forklaring = "Trenger mer info").shouldBeRight()

        test("tilbakestiller til TIL_BEHANDLING med ny behandletAv") {
            val tilbakestilt = sattPaVent().tilbakestill(NavIdent("DD3")).shouldBeRight()
            tilbakestilt.behandletAv shouldBe NavIdent("DD3")
            tilbakestilt.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
            tilbakestilt.besluttetAv shouldBe null
            tilbakestilt.besluttetTidspunkt shouldBe null
            tilbakestilt.forklaring shouldBe null
        }

        test("beholder ikke eksisterende årsaker etter tilbakestilling") {
            val opprettelse = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI, listOf("FEIL_BELOP"))
            val paVent = opprettelse.settPaVent(besluttetAv, listOf("FEIL_BELOP"), "Feil beløp").shouldBeRight()
            val tilbakestilt = paVent.tilbakestill(behandletAv).shouldBeRight()
            tilbakestilt.aarsaker shouldBe listOf()
            tilbakestilt.forklaring shouldBe null
        }

        test("oppdaterer behandletTidspunkt til nåtid") {
            val paVent = sattPaVent()
            val tilbakestilt = paVent.tilbakestill(behandletAv).shouldBeRight()
            tilbakestilt.behandletTidspunkt shouldNotBe paVent.behandletTidspunkt
        }

        test("feiler når status er TIL_BEHANDLING") {
            opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI).tilbakestill(behandletAv) shouldBeLeft TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent
        }

        test("feiler når status er GODKJENT") {
            val godkjent = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI).godkjenn(besluttetAv).shouldBeRight()
            godkjent.tilbakestill(behandletAv) shouldBeLeft TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent
        }

        test("feiler når status er RETURNERT") {
            val returnert = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI).returner(besluttetAv).shouldBeRight()
            returnert.tilbakestill(behandletAv) shouldBeLeft TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent
        }
    }
})
