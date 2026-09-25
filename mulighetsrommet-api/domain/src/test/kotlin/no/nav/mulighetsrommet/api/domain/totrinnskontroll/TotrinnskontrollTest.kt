package no.nav.mulighetsrommet.api.domain.totrinnskontroll

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
        behandletBegrunnelse: String? = null,
        behandletAarsaker: List<String> = emptyList(),
    ): Totrinnskontroll = Totrinnskontroll.opprett(
        id = UUID.randomUUID(),
        entityId = entityId,
        type = type,
        behandletAv = behandletAv,
        behandletBegrunnelse = behandletBegrunnelse,
        behandletAarsaker = behandletAarsaker,
    )

    context("opprett") {
        test("oppretter TIL_BEHANDLING med riktig behandletAv") {
            val opprettelse = opprett(TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            opprettelse.entityId shouldBe entityId
            opprettelse.behandling.utfortAv shouldBe behandletAv
            opprettelse.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
            opprettelse.beslutning shouldBe null
        }

        test("støtter årsaker og begrunnelse fra behandler") {
            val opprettelse = opprett(
                type = TotrinnskontrollType.TILSAGN_ANNULLERING,
                behandletBegrunnelse = "Perioden er feil",
                behandletAarsaker = listOf("FEIL_PERIODE"),
            )
            opprettelse.behandling.begrunnelse shouldBe "Perioden er feil"
            opprettelse.behandling.aarsaker shouldBe listOf("FEIL_PERIODE")
            opprettelse.beslutning shouldBe null
        }
    }

    context("godkjenn") {
        test("godkjenner og returnerer oppdatert tilstand") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.status shouldBe TotrinnskontrollStatus.GODKJENT
            godkjent.beslutning.shouldNotBeNull().utfortAv shouldBe besluttetAv
            godkjent.beslutning.tidspunkt shouldNotBe null
        }

        test("feiler når behandletAv og besluttetAv er samme NavIdent") {
            opprett().godkjenn(besluttetAv = behandletAv) shouldBeLeft TotrinnskontrollError.KanIkkeBesluttesAvBehandler
        }

        test("feiler når allerede godkjent") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.godkjenn(besluttetAv) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(TotrinnskontrollStatus.GODKJENT)
        }

        test("feiler når allerede returnert") {
            val returnert = opprett().returner(besluttetAv).shouldBeRight()
            returnert.godkjenn(besluttetAv) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(TotrinnskontrollStatus.RETURNERT)
        }

        test("godkjenning beholder behandlers årsaker") {
            val opprettelse = opprett(
                type = TotrinnskontrollType.TILSAGN_ANNULLERING,
                behandletAarsaker = listOf("FEIL_PERIODE"),
            )
            val godkjent = opprettelse.godkjenn(besluttetAv).shouldBeRight()
            godkjent.behandling.aarsaker shouldBe listOf("FEIL_PERIODE")
            godkjent.beslutning.shouldNotBeNull().aarsaker shouldBe emptyList()
        }

        test("godkjenning etter satt på vent fjerner forrige beslutters begrunnelse") {
            val sattPaVent = opprett()
                .settPaVent(besluttetAv, besluttetBegrunnelse = "Trenger mer informasjon")
                .shouldBeRight()

            val godkjent = sattPaVent.godkjenn(NavIdent("DD3")).shouldBeRight()

            godkjent.beslutning.shouldNotBeNull().begrunnelse shouldBe null
            godkjent.beslutning.shouldNotBeNull().aarsaker shouldBe emptyList()
        }
    }

    context("returner") {
        test("beholder behandlers begrunnelse og årsaker og legger til beslutters begrunnelse og årsaker") {
            val returnert = opprett(
                behandletBegrunnelse = "Begrunnelse fra behandler",
                behandletAarsaker = listOf("BEHANDLET_AARSAK"),
            ).returner(
                besluttetAv = besluttetAv,
                besluttetBegrunnelse = "Begrunnelse fra beslutter",
                besluttetAarsaker = listOf("BESLUTTET_AARSAK"),
            ).shouldBeRight()

            returnert.behandling.begrunnelse shouldBe "Begrunnelse fra behandler"
            returnert.behandling.aarsaker shouldBe listOf("BEHANDLET_AARSAK")
            returnert.beslutning.shouldNotBeNull().begrunnelse shouldBe "Begrunnelse fra beslutter"
            returnert.beslutning.aarsaker shouldBe listOf("BESLUTTET_AARSAK")
        }

        test("returnerer og oppdaterer tilstand") {
            val returnert = opprett().returner(
                besluttetAv = besluttetAv,
                besluttetBegrunnelse = "Beløp er feil",
                besluttetAarsaker = listOf("FEIL_BELOP"),
            ).shouldBeRight()
            returnert.status shouldBe TotrinnskontrollStatus.RETURNERT
            returnert.behandling.begrunnelse shouldBe null
            returnert.behandling.aarsaker shouldBe emptyList()
            returnert.beslutning.shouldNotBeNull().utfortAv shouldBe besluttetAv
            returnert.beslutning.tidspunkt shouldNotBe null
            returnert.beslutning.begrunnelse shouldBe "Beløp er feil"
            returnert.beslutning.aarsaker shouldBe listOf("FEIL_BELOP")
        }

        test("retur kan gjøres av samme NavIdent som behandletAv") {
            val returnert = opprett().returner(
                besluttetAv = behandletAv,
                besluttetBegrunnelse = "Beløp er feil",
                besluttetAarsaker = listOf("FEIL_BELOP"),
            ).shouldBeRight()
            returnert.status shouldBe TotrinnskontrollStatus.RETURNERT
            returnert.beslutning.shouldNotBeNull().utfortAv shouldBe behandletAv
        }

        test("feiler når allerede godkjent og besluttetAv er NavIdent") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.returner(
                besluttetAv,
                besluttetAarsaker = listOf("FEIL_BELOP"),
            ) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(
                TotrinnskontrollStatus.GODKJENT,
            )
        }

        test("feiler når allerede returnert") {
            val returnert = opprett().returner(besluttetAv).shouldBeRight()
            returnert.returner(
                besluttetAv,
                besluttetAarsaker = listOf("FEIL_BELOP"),
            ) shouldBeLeft TotrinnskontrollError.AlleredeBesluttet(
                TotrinnskontrollStatus.RETURNERT,
            )
        }

        test("systemet er tillatt å endre fra godkjent til returnert") {
            val godkjent = opprett().godkjenn(besluttetAv).shouldBeRight()
            godkjent.returner(Tiltaksadministrasjon, besluttetAarsaker = listOf("PROPAGERT_RETUR")).shouldBeRight()
        }
    }

    context("settPaVent") {
        test("setter på vent") {
            val paVent = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                .settPaVent(besluttetAv, besluttetBegrunnelse = "Trenger mer info")
                .shouldBeRight()
            paVent.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
            paVent.beslutning.shouldNotBeNull().utfortAv shouldBe besluttetAv
        }
    }

    context("tilbakestill") {
        fun sattPaVent(): Totrinnskontroll = opprett(TotrinnskontrollType.ENKELTPLASS_OKONOMI)
            .settPaVent(besluttetAv, besluttetBegrunnelse = "Trenger mer info").shouldBeRight()

        test("tilbakestiller til TIL_BEHANDLING med ny behandletAv") {
            val tilbakestilt = sattPaVent().tilbakestill(NavIdent("DD3")).shouldBeRight()
            tilbakestilt.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
            tilbakestilt.behandling.utfortAv shouldBe NavIdent("DD3")
            tilbakestilt.beslutning.shouldBeNull()
        }

        test("beholder ikke eksisterende årsaker etter tilbakestilling") {
            val opprettelse = opprett(
                type = TotrinnskontrollType.ENKELTPLASS_OKONOMI,
                behandletAarsaker = listOf("BEHANDLET_AARSAK"),
            )
            val paVent = opprettelse.settPaVent(
                besluttetAv = besluttetAv,
                besluttetBegrunnelse = "Feil beløp",
                besluttetAarsaker = listOf("BESLUTTET_AARSAK"),
            ).shouldBeRight()
            val tilbakestilt = paVent.tilbakestill(behandletAv).shouldBeRight()
            tilbakestilt.behandling.begrunnelse shouldBe null
            tilbakestilt.behandling.aarsaker shouldBe emptyList()
            tilbakestilt.beslutning.shouldBeNull()
        }

        test("oppdaterer behandletTidspunkt til nåtid") {
            val paVent = sattPaVent()
            val tilbakestilt = paVent.tilbakestill(behandletAv).shouldBeRight()
            tilbakestilt.behandling.tidspunkt shouldNotBe paVent.behandling.tidspunkt
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
