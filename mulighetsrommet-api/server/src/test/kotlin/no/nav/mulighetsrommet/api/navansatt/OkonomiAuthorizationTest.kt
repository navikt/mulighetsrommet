package no.nav.mulighetsrommet.api.navansatt

import arrow.core.nonEmptySetOf
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
    val annetKostnadssted = NavEnhetNummer("0500")

    fun medGenerellRolle(rolle: Rolle): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(rolle)))
    }

    fun medKontorspesifikkRolle(rolle: Rolle, enhet: NavEnhetNummer): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.kontorspesifikk(rolle, setOf(enhet))))
    }

    test("lesetilgang til tilsagn krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, false),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, false),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, false),
        ) { rolle, type, forventet ->
            OkonomiAuthorization.kanLeseTilsagn(medGenerellRolle(rolle), type) shouldBe forventet
        }
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.AVTALE) shouldBe false
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.ENKELTPLASS) shouldBe false
    }

    test("lesetilgang til utbetaling krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.AVTALE, false),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, true),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, false),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, false),
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
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
            GjennomforingType.AVTALE,
        ) shouldBe true
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
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

    test("økonomibeslutter godtar begge beslutningsroller for avtale og egen rolle for enkeltplass") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, true),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, true),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
        ) { rolle, type, enhet, forventet ->
            OkonomiAuthorization.erOkonomiBeslutter(
                medKontorspesifikkRolle(rolle, kostnadssted),
                OkonomiBeslutningContext(type, nonEmptySetOf(enhet)),
            ) shouldBe forventet
        }
    }

    test("tilsagnsbeslutter krever riktig rolle og kostnadssted for gjennomføringstypen") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, true),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
        ) { rolle, type, enhet, forventet ->
            OkonomiAuthorization.erBeslutterTilsagn(
                medKontorspesifikkRolle(rolle, kostnadssted),
                OkonomiBeslutningContext(type, nonEmptySetOf(enhet)),
            ) shouldBe forventet
        }
    }

    test("utbetalingsattestant krever riktig rolle og kostnadssted for gjennomføringstypen") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, true),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, true),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, false),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, false),
        ) { rolle, type, enhet, forventet ->
            OkonomiAuthorization.erAttestantUtbetaling(
                medKontorspesifikkRolle(rolle, kostnadssted),
                OkonomiBeslutningContext(type, nonEmptySetOf(enhet)),
            ) shouldBe forventet
        }
    }

    test("beslutning krever tilgang til alle kostnadssteder") {
        val beggeKostnadssteder = nonEmptySetOf(kostnadssted, annetKostnadssted)
        val avtale = OkonomiBeslutningContext(GjennomforingType.AVTALE, beggeKostnadssteder)
        val enkeltplass = OkonomiBeslutningContext(GjennomforingType.ENKELTPLASS, beggeKostnadssteder)

        val gruppetiltakBeslutter = medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, kostnadssted)
        val gruppetiltakAttestant = medKontorspesifikkRolle(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, kostnadssted)
        val enkeltplassBeslutter = medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, kostnadssted)

        OkonomiAuthorization.erBeslutterTilsagn(gruppetiltakBeslutter, avtale) shouldBe false
        OkonomiAuthorization.erAttestantUtbetaling(gruppetiltakAttestant, avtale) shouldBe false
        OkonomiAuthorization.erOkonomiBeslutter(gruppetiltakBeslutter, avtale) shouldBe false
        OkonomiAuthorization.erOkonomiBeslutter(gruppetiltakAttestant, avtale) shouldBe false
        OkonomiAuthorization.erBeslutterTilsagn(enkeltplassBeslutter, enkeltplass) shouldBe false
        OkonomiAuthorization.erAttestantUtbetaling(enkeltplassBeslutter, enkeltplass) shouldBe false
        OkonomiAuthorization.erOkonomiBeslutter(enkeltplassBeslutter, enkeltplass) shouldBe false

        val alleRoller = NavAnsattFixture.DonaldDuck.medRoller(
            setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, beggeKostnadssteder),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, beggeKostnadssteder),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, beggeKostnadssteder),
            ),
        )
        OkonomiAuthorization.erBeslutterTilsagn(alleRoller, avtale) shouldBe true
        OkonomiAuthorization.erAttestantUtbetaling(alleRoller, avtale) shouldBe true
        OkonomiAuthorization.erOkonomiBeslutter(alleRoller, avtale) shouldBe true
        OkonomiAuthorization.erBeslutterTilsagn(alleRoller, enkeltplass) shouldBe true
        OkonomiAuthorization.erAttestantUtbetaling(alleRoller, enkeltplass) shouldBe true
        OkonomiAuthorization.erOkonomiBeslutter(alleRoller, enkeltplass) shouldBe true
    }

    test("arena har ikke økonomitilgang uansett rolle") {
        val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
            setOf(
                NavAnsattRolle.generell(Rolle.OKONOMI_LES),
                NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
                NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, setOf(kostnadssted)),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, setOf(kostnadssted)),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, setOf(kostnadssted)),
            ),
        )

        OkonomiAuthorization.kanLeseTilsagn(ansatt, GjennomforingType.ARENA) shouldBe false
        OkonomiAuthorization.kanLeseUtbetaling(ansatt, GjennomforingType.ARENA) shouldBe false
        OkonomiAuthorization.erSaksbehandler(ansatt, GjennomforingType.ARENA) shouldBe false

        val kontekst = OkonomiBeslutningContext(GjennomforingType.ARENA, nonEmptySetOf(kostnadssted))
        OkonomiAuthorization.erOkonomiBeslutter(ansatt, kontekst) shouldBe false
        OkonomiAuthorization.erBeslutterTilsagn(ansatt, kontekst) shouldBe false
        OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst) shouldBe false
    }

    test("beslutter for enkeltplass må ha riktig rolle ved ansvarlig enhet") {
        val beslutterEnkeltplass = medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, kostnadssted)
        OkonomiAuthorization.erBeslutterEnkeltplass(beslutterEnkeltplass, setOf(kostnadssted)) shouldBe true
        OkonomiAuthorization.erBeslutterEnkeltplass(beslutterEnkeltplass, setOf(annetKostnadssted)) shouldBe false

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe false

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
            setOf(annetKostnadssted),
        ) shouldBe true
    }

    test("enkeltplass bruker egne økonomiroller") {
        OkonomiAuthorization.erSaksbehandlerEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        ) shouldBe true

        OkonomiAuthorization.erSaksbehandlerEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
        ) shouldBe false

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe false

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe true
    }
})
