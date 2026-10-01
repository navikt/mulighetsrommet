package no.nav.mulighetsrommet.api.navansatt

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.NavEnhetNummer

class OkonomiAuthorizationTest : FunSpec({
    val kostnadssted = NavEnhetNummer("0400")

    fun medGenerellRolle(rolle: Rolle): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(rolle)))
    }

    fun medKontorspesifikkRolle(rolle: Rolle, enhet: NavEnhetNummer): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.kontorspesifikk(rolle, setOf(enhet))))
    }

    test("lesetilgang til tilsagn krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, true),
            row(Rolle.SAKSBEHANDLER_OKONOMI, GjennomforingType.AVTALE, true),
            row(Rolle.BESLUTTER_TILSAGN, GjennomforingType.AVTALE, true),
            row(Rolle.ATTESTANT_UTBETALING, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, false),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.BESLUTTER_TILSAGN, GjennomforingType.ENKELTPLASS, false),
            row(Rolle.SAKSBEHANDLER_OKONOMI, GjennomforingType.ENKELTPLASS, false),
        ) { rolle, type, forventet ->
            OkonomiAuthorization.kanLeseTilsagn(medGenerellRolle(rolle), type) shouldBe forventet
        }
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.AVTALE) shouldBe false
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.ENKELTPLASS) shouldBe false
    }

    test("lesetilgang til utbetaling krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, true),
            row(Rolle.SAKSBEHANDLER_OKONOMI, GjennomforingType.AVTALE, true),
            row(Rolle.BESLUTTER_TILSAGN, GjennomforingType.AVTALE, true),
            row(Rolle.ATTESTANT_UTBETALING, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.AVTALE, false),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.ATTESTANT_UTBETALING, GjennomforingType.ENKELTPLASS, false),
            row(Rolle.SAKSBEHANDLER_OKONOMI, GjennomforingType.ENKELTPLASS, false),
        ) { rolle, type, forventet ->
            OkonomiAuthorization.kanLeseUtbetaling(medGenerellRolle(rolle), type) shouldBe forventet
        }
        OkonomiAuthorization.kanLeseUtbetaling(NavAnsattFixture.DonaldDuck, GjennomforingType.AVTALE) shouldBe false
        OkonomiAuthorization.kanLeseUtbetaling(
            NavAnsattFixture.DonaldDuck,
            GjennomforingType.ENKELTPLASS,
        ) shouldBe false
    }

    test("saksbehandler krever generell rolle i riktig saksområde") {
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI),
            GjennomforingType.AVTALE,
        ) shouldBe true
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe false
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe true
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
            GjennomforingType.AVTALE,
        ) shouldBe false
        OkonomiAuthorization.erSaksbehandler(
            medKontorspesifikkRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, kostnadssted),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe false
    }

    test("arena har ikke økonomitilgang uansett rolle") {
        val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
            setOf(
                NavAnsattRolle.generell(Rolle.OKONOMI_LES),
                NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
                NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted)),
                NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted)),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, setOf(kostnadssted)),
            ),
        )
        OkonomiAuthorization.kanLeseTilsagn(ansatt, GjennomforingType.ARENA) shouldBe false
        OkonomiAuthorization.kanLeseUtbetaling(ansatt, GjennomforingType.ARENA) shouldBe false
        OkonomiAuthorization.erSaksbehandler(ansatt, GjennomforingType.ARENA) shouldBe false
    }
})
