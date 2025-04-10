package no.nav.mulighetsrommet.api.oppgaver

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.oppgaver.Oppgave
import no.nav.mulighetsrommet.oppgaver.OppgaveType
import no.nav.mulighetsrommet.oppgaver.OppgaverService
import java.time.LocalDate
import java.time.LocalDateTime
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

            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(
                    Rolle.SAKSBEHANDLER_OKONOMI,
                    Rolle.BESLUTTER_TILSAGN,
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

            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(
                    Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
                    Rolle.BESLUTTER_TILSAGN,
                ),
            ).shouldBeEmpty()
        }

        test("Skal bare returnere oppgaver for tilsagn til godkjenning, annullering og til oppgjør når ansatt har korrekt rolle") {
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
            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(Rolle.BESLUTTER_TILSAGN),
            ).size shouldBe 3
        }

        test("Skal bare returnere oppgaver som er returnert til ansatte med SAKSBEHANDLER_OKONOMI") {
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
            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(Rolle.SAKSBEHANDLER_OKONOMI),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn3.id, OppgaveType.TILSAGN_RETURNERT),
            )
        }

        test("Skal bare returnere oppgaver for valgt kostnadssted") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                    TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.TIL_GODKJENNING)
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(Rolle.BESLUTTER_TILSAGN),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(TilsagnFixtures.Tilsagn2.id, OppgaveType.TILSAGN_TIL_GODKJENNING),
            )
        }

        test("Skal ikke se oppgaver hvis du ikke har korrekte roller") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.Oslo),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
                    TilsagnFixtures.Tilsagn3.copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
                ),
            ) {
                setTilsagnStatus(TilsagnFixtures.Tilsagn1, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_GODKJENNING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.TIL_GODKJENNING)
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            service.tilsagnOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(NavEnhetFixtures.Oslo.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = emptySet(),
            ).shouldBeEmpty()
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
                setDelutbetalingStatus(UtbetalingFixtures.delutbetaling1, DelutbetalingStatus.TIL_GODKJENNING)
                setDelutbetalingStatus(UtbetalingFixtures.delutbetaling2, DelutbetalingStatus.RETURNERT)
            }.initialize(database.db)

            service.delutbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(Rolle.ATTESTANT_UTBETALING),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.delutbetaling1.id, OppgaveType.UTBETALING_TIL_GODKJENNING),
            )

            service.delutbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                ansatt = NavAnsattFixture.MikkeMus.navIdent,
                roller = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.delutbetaling2.id, OppgaveType.UTBETALING_RETURNERT),
            )
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
                setDelutbetalingStatus(
                    UtbetalingFixtures.delutbetaling1,
                    DelutbetalingStatus.TIL_GODKJENNING,
                    behandletAv = NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.initialize(database.db)

            service.delutbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                ansatt = NavAnsattFixture.DonaldDuck.navIdent,
                roller = setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV, Rolle.ATTESTANT_UTBETALING),
            ).shouldBeEmpty()
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
                    TilsagnFixtures.Tilsagn1.copy(
                        id = UUID.randomUUID(),
                        gjennomforingId = VTA1.id,
                        kostnadssted = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                        lopenummer = 2,
                        bestillingsnummer = "A-2",
                    ),
                ),
                utbetalinger = listOf(
                    UtbetalingFixtures.utbetaling1.copy(
                        gjennomforingId = AFT1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                    UtbetalingFixtures.utbetaling2.copy(
                        gjennomforingId = AFT1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                    ),
                    UtbetalingFixtures.utbetaling3.copy(
                        gjennomforingId = VTA1.id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 3, 1)),
                    ),
                ),
                delutbetalinger = listOf(
                    UtbetalingFixtures.delutbetaling1.copy(utbetalingId = UtbetalingFixtures.utbetaling2.id),
                ),
            ) {
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling1.id,
                    LocalDateTime.now(),
                )
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling2.id,
                    LocalDateTime.now(),
                )
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling3.id,
                    LocalDateTime.now(),
                )
            }.initialize(database.db)

            val service = OppgaverService(database.db)

            service.utbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                roller = setOf(Rolle.SAKSBEHANDLER_OKONOMI),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling1.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
                PartialOppgave(UtbetalingFixtures.utbetaling3.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )

            service.utbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                roller = setOf(Rolle.SAKSBEHANDLER_OKONOMI),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling3.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )

            service.utbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(),
                kostnadssteder = setOf(),
                roller = setOf(Rolle.ATTESTANT_UTBETALING),
            ).shouldBeEmpty()

            service.utbetalingOppgaver(
                oppgavetyper = setOf(),
                tiltakskoder = setOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
                kostnadssteder = setOf(),
                roller = setOf(Rolle.SAKSBEHANDLER_OKONOMI),
            ) shouldMatchAllOppgaver listOf(
                PartialOppgave(UtbetalingFixtures.utbetaling1.id, OppgaveType.UTBETALING_TIL_BEHANDLING),
            )
        }
    }
})

data class PartialOppgave(val id: UUID, val type: OppgaveType)

infix fun Collection<Oppgave>.shouldMatchAllOppgaver(expectedOppgaver: List<PartialOppgave>) {
    this.size shouldBe expectedOppgaver.size
    expectedOppgaver.forEach { expected ->
        this.shouldHaveSingleElement { oppgave ->
            oppgave.id == expected.id && oppgave.type == expected.type
        }
    }
}
