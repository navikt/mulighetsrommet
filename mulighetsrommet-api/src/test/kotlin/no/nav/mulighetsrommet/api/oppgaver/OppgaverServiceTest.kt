package no.nav.mulighetsrommet.api.oppgaver

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.setDelutbetalingStatus
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
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
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    TilsagnFixtures.Tilsagn1,
                ),
            )

            domain.initialize(database.db)

            val oppgaver = service.tilsagnOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(
                    NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
                    NavAnsattRolle.OKONOMI_BESLUTTER,
                ),
            )

            oppgaver.size shouldBe 1
        }

        test("Skal bare returnere oppgaver for tilsagn til godkjenning og annullering når ansatt har korrekt rolle") {
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
                setTilsagnStatus(TilsagnFixtures.Tilsagn2, TilsagnStatus.TIL_ANNULLERING)
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.ANNULLERT)
            }.initialize(database.db)

            val service = OppgaverService(database.db)
            service.tilsagnOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.OKONOMI_BESLUTTER),
            ).size shouldBe 2
        }

        test("Skal bare returnere oppgaver som er returnert til ansatte uten beslutterrolle") {
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
                setTilsagnStatus(TilsagnFixtures.Tilsagn3, TilsagnStatus.RETURNERT)
            }.initialize(database.db)

            val service = OppgaverService(database.db)
            service.tilsagnOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
            ).size shouldBe 1
        }

        test("Skal bare returnere oppgaver for valgt region") {
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
            ).initialize(database.db)

            val service = OppgaverService(database.db)

            service.tilsagnOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                roller = setOf(NavAnsattRolle.OKONOMI_BESLUTTER),
            ).size shouldBe 1
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
            ).initialize(database.db)

            val service = OppgaverService(database.db)

            val oppgaver = service.tilsagnOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = listOf(NavEnhetFixtures.Oslo.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
                roller = emptySet(),
            )

            oppgaver.size shouldBe 0
        }
    }

    context("delutbetalinger") {
        test("Skal hente oppgaver for delutbetalinger med filter") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
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
                setDelutbetalingStatus(
                    UtbetalingFixtures.delutbetaling2,
                    UtbetalingFixtures.DelutbetalingStatus.RETURNERT,
                )
            }.initialize(database.db)

            var oppgaver = service.delutbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV, NavAnsattRolle.OKONOMI_BESLUTTER),
            )
            oppgaver.size shouldBe 2

            oppgaver = service.delutbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
            )
            oppgaver.size shouldBe 1
            oppgaver[0].type shouldBe OppgaveType.UTBETALING_RETURNERT

            oppgaver = service.delutbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = listOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV, NavAnsattRolle.OKONOMI_BESLUTTER),
            )
            oppgaver.size shouldBe 1
        }
    }

    context("utbetalinger") {
        test("Skal hente oppgaver for utbetalinger med filter") {
            val service = OppgaverService(database.db)
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
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
                    TilsagnFixtures.Tilsagn1,
                    TilsagnFixtures.Tilsagn1.copy(
                        id = UUID.randomUUID(),
                        gjennomforingId = UtbetalingFixtures.utbetaling3.gjennomforingId,
                        kostnadssted = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                        bestillingsnummer = "A-2025/1-4",
                    ),
                ),
                utbetalinger = listOf(
                    UtbetalingFixtures.utbetaling1.copy(periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1))),
                    UtbetalingFixtures.utbetaling2,
                    UtbetalingFixtures.utbetaling3,
                ),
                delutbetalinger = listOf(
                    UtbetalingFixtures.delutbetaling1.copy(utbetalingId = UtbetalingFixtures.utbetaling2.id),
                ),
            ).initialize(database.db)
            database.run {
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling1.id,
                    LocalDateTime.now(),
                )
            }
            database.run {
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling2.id,
                    LocalDateTime.now(),
                )
            }
            database.run {
                queries.utbetaling.setGodkjentAvArrangor(
                    UtbetalingFixtures.utbetaling3.id,
                    LocalDateTime.now(),
                )
            }

            service.utbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
            ).size shouldBe 2

            service.utbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = listOf(NavEnhetFixtures.TiltakOslo.enhetsnummer),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
            ).size shouldBe 1

            service.utbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = emptyList(),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.OKONOMI_BESLUTTER),
            ).size shouldBe 0

            service.utbetalingOppgaver(
                oppgavetyper = emptyList(),
                tiltakskoder = listOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
                kostnadssteder = emptyList(),
                roller = setOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
            ).size shouldBe 1
        }
    }
})
