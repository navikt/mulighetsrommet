package no.nav.mulighetsrommet.api.oppgaver

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.oppgaver.Oppgave
import no.nav.mulighetsrommet.oppgaver.OppgaveType
import no.nav.mulighetsrommet.oppgaver.OppgaverService
import java.time.LocalDate
import java.util.*

class OppgaverServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("tilsagn") {
        test("Skal hente oppgaver for tilsagn med filter") {
            val service = OppgaverService(database.db)

            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ) {
                setTilsagnStatus(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnStatus.TIL_GODKJENNING,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.BESLUTTER_TILSAGN),
                ),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn1.id, OppgaveType.TILSAGN_TIL_GODKJENNING),
            )
        }

        test("Skal ikke se oppgaver som ansatt selv har sendt til godkjenning") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ) {
                setTilsagnStatus(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnStatus.TIL_GODKJENNING,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
                    NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                    NavAnsattRolle.generell(Rolle.BESLUTTER_TILSAGN),
                ),
            ).shouldBeEmpty()
        }

        test("Skal bare se oppgaver for tilsagn til godkjenning, annullering og til oppgjør når ansatt har beslutter-rolle") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn2,
                    TilsagnFixtures.Tilsagn3,
                    TilsagnFixtures.Tilsagn4,
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_ANNULLERING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.ANNULLERT)
                setTilsagnStatus(TilsagnFixtures.Tilsagn4, TilsagnStatus.TIL_OPPGJOR)
            }.initialize(database.db)

            val service = OppgaverService(database.db)
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.BESLUTTER_TILSAGN)),
            ).size shouldBe 3
        }

        test("Skal bare se oppgaver som er returnert når ansatt har saksbehandler-rolle") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn2,
                    TilsagnFixtures.Tilsagn3,
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.RETURNERT)
            }.initialize(database.db)

            val service = OppgaverService(database.db)
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn3.id, OppgaveType.TILSAGN_RETURNERT),
            )
        }

        test("Skal bare se oppgaver for valgt region") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1.copy(kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer),
                    TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                    TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.TIL_GODKJENNING)
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.BESLUTTER_TILSAGN)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn1.id, OppgaveType.TILSAGN_TIL_GODKJENNING),
                PartialOppgave(TilsagnFixtures.Tilsagn2.id, OppgaveType.TILSAGN_TIL_GODKJENNING),
            )
        }

        test("Skal se oppgaver for returnert tilsagn når ansatt selv har sendt tilsagn til godkjenning") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
            ) {
                setTilsagnStatus(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnStatus.RETURNERT,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                ),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn1.id, OppgaveType.TILSAGN_RETURNERT),
            )
        }

        test("Skal bare se oppgaver for kostnadssteder som overlapper med ansattes roller") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1.copy(kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer),
                    TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                    TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.TIL_GODKJENNING)
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.BESLUTTER_TILSAGN,
                        setOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                    ),
                ),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn2.id, OppgaveType.TILSAGN_TIL_GODKJENNING),
            )
        }
    }

    context("delutbetalinger") {
        test("Skal hente oppgaver for delutbetalinger med filter") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                navEnheter = listOf(
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ),
                utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
                delutbetalinger = listOf(
                    UtbetalingFixtures.delutbetaling1,
                    UtbetalingFixtures.delutbetaling2,
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(UtbetalingFixtures.delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setDelutbetalingStatus(UtbetalingFixtures.delutbetaling2, DelutbetalingStatus.RETURNERT)
            }.initialize(database.db)

            // Skal se utbetaling til godkjenning når ansatt har attestant-rolle
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.delutbetaling1.id, OppgaveType.UTBETALING_TIL_ATTESTERING),
            )

            // Skal se returnert utbetaling når ansatt har saksbehandler-rolle
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                ),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.delutbetaling2.id, OppgaveType.UTBETALING_RETURNERT),
            )

            // Skal se returnert utbetaling når ansatt selv var den som sendte til godkjenning
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI),
                ),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.delutbetaling2.id, OppgaveType.UTBETALING_RETURNERT),
            )

            // Skal ikke se returnert utbetaling når ansatt ikke har saksbehandler-rolle
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.BESLUTTER_TILSAGN),
                ),
            ).shouldBeEmpty()
        }

        test("Skal ikke hente oppgaver som ansatt selv har sendt til godkjenning") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
                delutbetalinger = listOf(UtbetalingFixtures.delutbetaling1),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(
                    UtbetalingFixtures.delutbetaling1,
                    DelutbetalingStatus.TIL_ATTESTERING,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                ),
            ).shouldBeEmpty()
        }

        test("Skal ikke hente oppgaver som ansatt selv har besluttet tilsagnet") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
                delutbetalinger = listOf(UtbetalingFixtures.delutbetaling1),
            ) {
                setTilsagnStatus(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnStatus.GODKJENT,
                    besluttetAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
                setDelutbetalingStatus(
                    UtbetalingFixtures.delutbetaling1,
                    DelutbetalingStatus.TIL_ATTESTERING,
                    behandletAv = NavAnsattFixture.MikkeMus.navIdent,
                )
            }.initialize(database.db)

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                ),
            ).shouldBeEmpty()
            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.FetterAnton.navIdent,
                roller = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                ),
            ).shouldHaveSize(1)
        }

        test("Skal bare se oppgaver for kostandssteder som overlapper med ansattes roller") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
                delutbetalinger = listOf(UtbetalingFixtures.delutbetaling1),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(
                    UtbetalingFixtures.delutbetaling1,
                    DelutbetalingStatus.TIL_ATTESTERING,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            forAll(
                row(
                    NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
                    listOf(),
                ),
                row(
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                    listOf(
                        PartialOppgave(UtbetalingFixtures.delutbetaling1.id, OppgaveType.UTBETALING_TIL_ATTESTERING),
                    ),
                ),
                row(
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.ATTESTANT_UTBETALING,
                        setOf(NavEnhetFixtures.Oslo.enhetsnummer),
                    ),
                    listOf(),
                ),
                row(
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.ATTESTANT_UTBETALING,
                        setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
                    ),
                    listOf(
                        PartialOppgave(UtbetalingFixtures.delutbetaling1.id, OppgaveType.UTBETALING_TIL_ATTESTERING),
                    ),
                ),
            ) { rolle, expectedOppgaver ->
                service.oppgaver(
                    oppgavetyper = setOf(),
                    tiltakskoder = setOf(),
                    regioner = setOf(),
                    ansatt = NavAnsattFixture.MikkeMus.navIdent,
                    roller = setOf(rolle),
                ) shouldMatchAllOppgaver expectedOppgaver
            }
        }
    }

    context("utbetalinger") {
        test("Skal hente oppgaver for utbetalinger med filter") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.VTA),
                navEnheter = listOf(
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
                gjennomforinger = listOf(AFT1, VTA1),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1.copy(
                        gjennomforingId = AFT1.id,
                        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
                        lopenummer = 1,
                        bestillingsnummer = "A-1",
                    ),
                    TilsagnFixtures.Tilsagn2.copy(
                        gjennomforingId = VTA1.id,
                        kostnadssted = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                        lopenummer = 2,
                        bestillingsnummer = "A-2",
                    ),
                ),
                utbetalinger = listOf(
                    UtbetalingFixtures.utbetaling1.copy(
                        status = UtbetalingStatusType.INNSENDT,
                        gjennomforingId = AFT1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        innsender = Arrangor,
                    ),
                    UtbetalingFixtures.utbetaling2.copy(
                        status = UtbetalingStatusType.FERDIG_BEHANDLET,
                        gjennomforingId = AFT1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                        innsender = Arrangor,
                    ),
                    UtbetalingFixtures.utbetaling3.copy(
                        status = UtbetalingStatusType.INNSENDT,
                        gjennomforingId = VTA1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
                        innsender = Arrangor,
                    ),
                ),
                delutbetalinger = listOf(
                    UtbetalingFixtures.delutbetaling1.copy(utbetalingId = UtbetalingFixtures.utbetaling2.id),
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(UtbetalingFixtures.delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            val oppgaver = service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI), NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("0100")))),
            )
            oppgaver shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling1.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
                PartialOppgave(UtbetalingFixtures.utbetaling3.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling3.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING)),
            ) shouldHaveSize 0

            service.oppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.SAKSBEHANDLER_OKONOMI)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling1.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )
        }
    }

    context("avtaler") {
        test("oppgaver når aktive avtaler mangler administrator") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(
                    AvtaleFixtures.AFT.copy(
                        administratorer = listOf(),
                        status = AvtaleStatusType.AKTIV,
                    ),
                    AvtaleFixtures.gruppeAmo.copy(
                        administratorer = listOf(),
                        status = AvtaleStatusType.AVSLUTTET,
                    ),
                    AvtaleFixtures.VTA.copy(
                        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                        status = AvtaleStatusType.AKTIV,
                    ),
                ),
            ).initialize(database.db)

            val service = OppgaverService(database.db)

            service.oppgaver(
                oppgavetyper = setOf(OppgaveType.AVTALE_MANGLER_ADMINISTRATOR),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.AVTALER_SKRIV)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(
                    AvtaleFixtures.AFT.id,
                    OppgaveType.AVTALE_MANGLER_ADMINISTRATOR,
                ),
            )
        }
    }

    context("gjennomføringer") {
        test("oppgaver når aktive gjennomføringer mangler administrator") {
            val avsluttetGjennomforing = UUID.randomUUID()
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(
                    AFT1.copy(
                        status = GjennomforingStatusType.GJENNOMFORES,
                        administratorer = listOf(),
                    ),
                    AFT1.copy(
                        id = avsluttetGjennomforing,
                        status = GjennomforingStatusType.AVSLUTTET,
                        administratorer = listOf(),
                    ),
                    AFT1.copy(
                        id = UUID.randomUUID(),
                        status = GjennomforingStatusType.GJENNOMFORES,
                        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                    ),
                ),
            ).initialize(database.db)

            val service = OppgaverService(database.db)

            service.oppgaver(
                oppgavetyper = setOf(OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR),
                tiltakskoder = setOf(),
                regioner = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(
                    AFT1.id,
                    OppgaveType.GJENNOMFORING_MANGLER_ADMINISTRATOR,
                ),
            )
        }
    }
})

private data class PartialOppgave(val id: UUID, val type: OppgaveType)

private infix fun Collection<Oppgave>.shouldMatchAllOppgaver(expectedOppgaver: List<PartialOppgave>) {
    expectedOppgaver.forEach { expected ->
        shouldHaveSingleElement { oppgave ->
            oppgave.id == expected.id && oppgave.type == expected.type
        }
    }
    this.size shouldBe expectedOppgaver.size
}
