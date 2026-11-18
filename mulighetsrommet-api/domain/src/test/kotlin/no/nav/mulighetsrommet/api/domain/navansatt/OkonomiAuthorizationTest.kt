package no.nav.mulighetsrommet.api.domain.navansatt

import arrow.core.left
import arrow.core.nonEmptySetOf
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.model.NavEnhetNummer

class OkonomiAuthorizationTest : FunSpec({
    val kostnadssted = NavEnhetNummer("0400")
    val annetKostnadssted = NavEnhetNummer("0500")
    val manglerTilgangEnkeltplass = OkonomiAuthorizationError.ManglerTilgang(setOf(Rolle.OKONOMI_LES, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)).left()
    val manglerTilgangGruppe = OkonomiAuthorizationError.ManglerTilgang(setOf(Rolle.OKONOMI_LES, Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)).left()
    val invalidState = OkonomiAuthorizationError.InvalidState().left()

    fun harTilgang(vararg roller: Rolle) = HarTilgang(setOf(*roller)).right()
    fun manglerTilgang(vararg roller: Rolle) = OkonomiAuthorizationError.ManglerTilgang(setOf(*roller)).left()
    fun manglerKontorspesifikkTilgang(vararg roller: Rolle) = OkonomiAuthorizationError.ManglerKontorspesifikkTilgang(setOf(*roller)).left()

    fun medGenerellRolle(rolle: Rolle): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(rolle)))
    }

    fun medKontorspesifikkRolle(rolle: Rolle, enhet: NavEnhetNummer): NavAnsatt {
        return NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.kontorspesifikk(rolle, setOf(enhet))))
    }

    test("lesetilgang til tilsagn krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_LES)),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, manglerTilgangGruppe),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_LES)),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, manglerTilgangEnkeltplass),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, manglerTilgangEnkeltplass),
        ) { rolle, type, forventet ->
            OkonomiAuthorization.kanLeseTilsagn(medGenerellRolle(rolle), type) shouldBe forventet
        }
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.AVTALE) shouldBe manglerTilgangGruppe
        OkonomiAuthorization.kanLeseTilsagn(NavAnsattFixture.DonaldDuck, GjennomforingType.ENKELTPLASS) shouldBe manglerTilgangEnkeltplass
    }

    test("lesetilgang til utbetaling krever rolle for riktig gjennomføringstype") {
        forAll(
            row(Rolle.OKONOMI_LES, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_LES)),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, harTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.AVTALE, manglerTilgangGruppe),
            row(Rolle.OKONOMI_LES, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_LES)),
            row(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, manglerTilgangEnkeltplass),
            row(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, manglerTilgangEnkeltplass),
        ) { rolle, type, forventet ->
            OkonomiAuthorization.kanLeseUtbetaling(medGenerellRolle(rolle), type) shouldBe forventet
        }
        OkonomiAuthorization.kanLeseUtbetaling(NavAnsattFixture.DonaldDuck, GjennomforingType.AVTALE) shouldBe manglerTilgangGruppe
        OkonomiAuthorization.kanLeseUtbetaling(
            NavAnsattFixture.DonaldDuck,
            GjennomforingType.ENKELTPLASS,
        ) shouldBe manglerTilgangEnkeltplass
    }

    test("saksbehandler krever generell rolle i riktig saksområde") {
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
            GjennomforingType.AVTALE,
        ) shouldBe harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe manglerTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)
        OkonomiAuthorization.erSaksbehandler(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
            GjennomforingType.AVTALE,
        ) shouldBe manglerTilgang(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)
        OkonomiAuthorization.erSaksbehandler(
            medKontorspesifikkRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS, kostnadssted),
            GjennomforingType.ENKELTPLASS,
        ) shouldBe manglerTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)
    }

    test("økonomibeslutter godtar begge beslutningsroller for avtale og egen rolle for enkeltplass") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, harTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
        ) { rolle, type, enhet, forventet ->
            OkonomiAuthorization.erOkonomiBeslutter(
                medKontorspesifikkRolle(rolle, kostnadssted),
                OkonomiBeslutningContext(type, nonEmptySetOf(enhet)),
            ) shouldBe forventet
        }
    }

    test("tilsagnsbeslutter krever riktig rolle og kostnadssted for gjennomføringstypen") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
        ) { rolle, type, enhet, forventet ->
            OkonomiAuthorization.erBeslutterTilsagn(
                medKontorspesifikkRolle(rolle, kostnadssted),
                OkonomiBeslutningContext(type, nonEmptySetOf(enhet)),
            ) shouldBe forventet
        }
    }

    test("utbetalingsattestant krever riktig rolle og kostnadssted for gjennomføringstypen") {
        forAll(
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, kostnadssted, harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ENKELTPLASS, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.AVTALE, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, kostnadssted, harTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.AVTALE, annetKostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ENKELTPLASS, kostnadssted, manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)),
            row(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, GjennomforingType.ARENA, kostnadssted, invalidState),
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

        OkonomiAuthorization.erBeslutterTilsagn(gruppetiltakBeslutter, avtale) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)
        OkonomiAuthorization.erAttestantUtbetaling(gruppetiltakAttestant, avtale) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)
        OkonomiAuthorization.erOkonomiBeslutter(gruppetiltakBeslutter, avtale) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)
        OkonomiAuthorization.erOkonomiBeslutter(gruppetiltakAttestant, avtale) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)
        OkonomiAuthorization.erBeslutterTilsagn(enkeltplassBeslutter, enkeltplass) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
        OkonomiAuthorization.erAttestantUtbetaling(enkeltplassBeslutter, enkeltplass) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
        OkonomiAuthorization.erOkonomiBeslutter(enkeltplassBeslutter, enkeltplass) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)

        val alleRoller = NavAnsattFixture.DonaldDuck.medRoller(
            setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, beggeKostnadssteder),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, beggeKostnadssteder),
                NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, beggeKostnadssteder),
            ),
        )
        OkonomiAuthorization.erBeslutterTilsagn(alleRoller, avtale) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)
        OkonomiAuthorization.erAttestantUtbetaling(alleRoller, avtale) shouldBe harTilgang(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)
        OkonomiAuthorization.erOkonomiBeslutter(alleRoller, avtale) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK)
        OkonomiAuthorization.erBeslutterTilsagn(alleRoller, enkeltplass) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
        OkonomiAuthorization.erAttestantUtbetaling(alleRoller, enkeltplass) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
        OkonomiAuthorization.erOkonomiBeslutter(alleRoller, enkeltplass) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
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

        OkonomiAuthorization.kanLeseTilsagn(ansatt, GjennomforingType.ARENA) shouldBe invalidState
        OkonomiAuthorization.kanLeseUtbetaling(ansatt, GjennomforingType.ARENA) shouldBe invalidState
        OkonomiAuthorization.erSaksbehandler(ansatt, GjennomforingType.ARENA) shouldBe invalidState

        val kontekst = OkonomiBeslutningContext(GjennomforingType.ARENA, nonEmptySetOf(kostnadssted))
        OkonomiAuthorization.erOkonomiBeslutter(ansatt, kontekst) shouldBe invalidState
        OkonomiAuthorization.erBeslutterTilsagn(ansatt, kontekst) shouldBe invalidState
        OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst) shouldBe invalidState
    }

    test("beslutter for enkeltplass må ha riktig rolle ved ansvarlig enhet") {
        val beslutterEnkeltplass = medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, kostnadssted)
        OkonomiAuthorization.erBeslutterEnkeltplass(beslutterEnkeltplass, setOf(kostnadssted)) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
        OkonomiAuthorization.erBeslutterEnkeltplass(beslutterEnkeltplass, setOf(annetKostnadssted)) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_GRUPPETILTAK, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS),
            setOf(annetKostnadssted),
        ) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
    }

    test("enkeltplass bruker egne økonomiroller") {
        OkonomiAuthorization.erSaksbehandlerEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS),
        ) shouldBe harTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)

        OkonomiAuthorization.erSaksbehandlerEnkeltplass(
            medGenerellRolle(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK),
        ) shouldBe manglerTilgang(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe manglerKontorspesifikkTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)

        OkonomiAuthorization.erBeslutterEnkeltplass(
            medKontorspesifikkRolle(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, kostnadssted),
            setOf(kostnadssted),
        ) shouldBe harTilgang(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)
    }
})
