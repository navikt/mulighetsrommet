package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn2
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.delutbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling2
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.DelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.LocalDate
import java.util.*

class UtbetalingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    fun createUtbetalingService(
        tilsagnService: TilsagnService = mockk(relaxed = true),
        journalforUtbetaling: JournalforUtbetaling = mockk(relaxed = true),
    ) = UtbetalingService(
        config = UtbetalingService.Config(
            bestillingTopic = "topic",
        ),
        db = database.db,
        tilsagnService = tilsagnService,
        journalforUtbetaling = journalforUtbetaling,
    )

    context("når utbetaling blir behandlet") {
        test("skal ikke kunne beslutte delutbetaling når ansatt mangler attestant-rolle") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = domain.ansatte[1].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (Nav Innlandet)"),
            )
        }

        test("kan ikke beslutte egen utbetaling") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            )
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet"),
            )
        }

        test("kan ikke beslutte utbetaling når man har besluttet tilsagnet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT, besluttetAv = NavAnsattFixture.DonaldDuck.navIdent)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            )
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke attestere en utbetaling der du selv har besluttet tilsagnet"),
            )
        }

        test("skal ikke kunne opprette delutbetaling hvis utbetalingen allerede er godkjent") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling = DelutbetalingRequest(UUID.randomUUID(), Tilsagn1.id, gjorOppTilsagn = false, belop = 10)
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError("/0", "Utbetaling kan ikke endres fordi den har status: OVERFORT_TIL_UTBETALING"),
            )
        }

        test("returnering av delutbetaling setter den i RETURNERT status") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight()
            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT
        }

        test("sletting av delutbetaling skjer ikke ved valideringsfeil") {
            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()
            val delutbetaling = DelutbetalingRequest(
                id = UUID.randomUUID(),
                tilsagnId = Tilsagn1.id,
                gjorOppTilsagn = false,
                belop = 100,
            )
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                id = delutbetaling.id,
                request = BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(
                    aarsaker = emptyList(),
                    forklaring = null,
                ),
                navIdent = domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(utbetaling1.id, emptyList()),
                navIdent = domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetalingslinjer mangler"),
            )

            database.run {
                queries.delutbetaling.get(delutbetaling.id).shouldNotBeNull()
            }
        }

        test("skal ikke kunne godkjenne delutbetaling hvis den er allerede godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling er ikke satt til attestering"),
            )
        }

        test("oppdatering av returnert delutbetaling setter status TIL_ATTESTERING") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.RETURNERT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.opprettDelutbetalinger(
                request = OpprettDelutbetalingerRequest(
                    utbetalingId = utbetaling1.id,
                    delutbetalinger = listOf(
                        DelutbetalingRequest(delutbetaling1.id, Tilsagn1.id, gjorOppTilsagn = false, belop = 100),
                    ),
                ),
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.get(delutbetaling1.id).shouldNotBeNull()
                    .status shouldBe DelutbetalingStatus.TIL_ATTESTERING
            }
        }

        test("skal bare kunne opprette delutbetaling når utbetalingsperiode og tilsagnsperiode overlapper") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2023, 4, 4)),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            val request = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(
                    DelutbetalingRequest(UUID.randomUUID(), Tilsagn1.id, gjorOppTilsagn = false, belop = 100),
                ),
            )

            shouldThrow<IllegalArgumentException> {
                service.opprettDelutbetalinger(
                    request,
                    NavAnsattFixture.DonaldDuck.navIdent,
                )
            }.message shouldBe "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        test("skal ikke kunne opprette delutbetaling hvis beløp er for stort") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )

            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)
            val service = createUtbetalingService()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 100)),
                ),
                domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 7),
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 5),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("ny send til godkjenning sletter rader som ikke er med") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val delutbetaling1 = DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 5)
            val delutbetaling2 = DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 5)
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1, delutbetaling2)),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                delutbetaling1.id,
                BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(emptyList(), null),
                domain.ansatte[1].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1)),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            val delutbetalinger = database.run { queries.delutbetaling.getByUtbetalingId(utbetaling.id) }
            delutbetalinger.size shouldBe 1
            delutbetalinger[0].id shouldBe delutbetaling1.id
        }

        test("alle delutbetalinger (selv godkjente) blir returnert når saksbehandler avviser en delutbetaling") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            )
            val utbetaling = utbetaling1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                beregning = UtbetalingBeregningFri(
                    input = UtbetalingBeregningFri.Input(10),
                    output = UtbetalingBeregningFri.Output(10),
                ),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.DonaldDuck,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)
            val service = createUtbetalingService()

            val delutbetaling1 = DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 5)
            val delutbetaling2 = DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 5)
            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(utbetaling.id, listOf(delutbetaling1, delutbetaling2)),
                domain.ansatte[1].navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                delutbetaling1.id,
                BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.GODKJENT

            service.besluttDelutbetaling(
                delutbetaling2.id,
                BesluttDelutbetalingRequest.AvvistDelutbetalingRequest(emptyList(), null),
                domain.ansatte[0].navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                queries.delutbetaling.getOrError(delutbetaling1.id).status shouldBe DelutbetalingStatus.RETURNERT
                queries.totrinnskontroll.getOrError(delutbetaling1.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.totrinnskontroll.getOrError(delutbetaling2.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe domain.ansatte[0].navIdent
                }
            }
        }

        test("løpenummer, fakturanummer og periode blir utledet fra tilsagnet og utbetalingen") {
            val tilsagn1 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            val utbetaling1 = utbetaling1.copy(
                periode = Periode(LocalDate.of(2023, 12, 15), LocalDate.of(2025, 1, 15)),
            )

            val utbetaling2 = utbetaling2.copy(
                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15)),
            )

            val domain = MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1, utbetaling2),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling1.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 50),
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 50),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            service.opprettDelutbetalinger(
                OpprettDelutbetalingerRequest(
                    utbetaling2.id,
                    listOf(
                        DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 100),
                    ),
                ),
                domain.ansatte[0].navIdent,
            ).shouldBeRight()

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).should { (first, second) ->
                    first.belop shouldBe 50
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    first.lopenummer shouldBe 1
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-1"

                    second.belop shouldBe 50
                    second.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))
                    second.lopenummer shouldBe 1
                    second.faktura.fakturanummer shouldBe "A-2025/1-2-1"
                }

                queries.delutbetaling.getByUtbetalingId(utbetaling2.id).should { (first) ->
                    first.belop shouldBe 100
                    first.lopenummer shouldBe 2
                    first.faktura.fakturanummer shouldBe "A-2025/1-1-2"
                    first.periode shouldBe Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1))
                }
            }
        }

        test("delutbetaling blir returnert hvis tilsagn har endret status") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.OPPGJORT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.TIL_ATTESTERING)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.RETURNERT

            database.run {
                val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling1.id, Totrinnskontroll.Type.OPPRETT)
                opprettelse.besluttetAv shouldBe Tiltaksadministrasjon
                opprettelse.besluttelse shouldBe Besluttelse.AVVIST
            }
        }

        test("utbetaling blir sendt som melding til økonomi når alle delutbetalinger er godkjent") {
            val tilsagn1 = Tilsagn1.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-1",
            )

            val tilsagn2 = Tilsagn2.copy(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                bestillingsnummer = "A-2025/1-2",
            )

            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn1, tilsagn2),
                utbetalinger = listOf(utbetaling1),
            ) {
                setTilsagnStatus(tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(tilsagn2, TilsagnStatus.GODKJENT)
                setRoller(
                    NavAnsattFixture.MikkeMus,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(Innlandet.enhetsnummer))),
                )
            }.initialize(database.db)

            val service = createUtbetalingService()

            val delutbetaling1 = DelutbetalingRequest(UUID.randomUUID(), tilsagn1.id, gjorOppTilsagn = false, belop = 1)
            val delutbetaling2 = DelutbetalingRequest(UUID.randomUUID(), tilsagn2.id, gjorOppTilsagn = false, belop = 2)
            val opprettRequest = OpprettDelutbetalingerRequest(
                utbetalingId = utbetaling1.id,
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            )
            service.opprettDelutbetalinger(
                request = opprettRequest,
                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
            ).shouldBeRight()

            service.besluttDelutbetaling(
                id = delutbetaling1.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.GODKJENT

            service.besluttDelutbetaling(
                id = delutbetaling2.id,
                request = BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                navIdent = NavAnsattFixture.MikkeMus.navIdent,
            ).shouldBeRight().status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING

            database.run {
                val records = queries.kafkaProducerRecord.getRecords(10)
                records.shouldHaveSize(2)

                Json.decodeFromString<OkonomiBestillingMelding>(records[0].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.fakturanummer shouldBe "A-2025/1-1-1"
                        it.belop shouldBe 1
                        it.behandletAv shouldBe NavAnsattFixture.DonaldDuck.navIdent.toOkonomiPart()
                        it.besluttetAv shouldBe NavAnsattFixture.MikkeMus.navIdent.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                    }

                Json.decodeFromString<OkonomiBestillingMelding>(records[1].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.fakturanummer shouldBe "A-2025/1-2-1"
                        it.belop shouldBe 2
                        it.behandletAv shouldBe NavAnsattFixture.DonaldDuck.navIdent.toOkonomiPart()
                        it.besluttetAv shouldBe NavAnsattFixture.MikkeMus.navIdent.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                    }
            }
        }
    }

    context("Automatisk utbetaling") {
        val utbetaling1Id = utbetaling1.id

        val utbetaling1Forhandsgodkjent = utbetaling1.copy(
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            beregning = getForhandsgodkjentBeregning(
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                belop = 1000,
            ),
        )

        val tilsagnBeregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                linjer = listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "Beskrivelse",
                        belop = 1500,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(1500),
        )

        test("utbetales ikke automatisk hvis det allerede finnes en delutbetaling når arrangør godkjenner") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
                delutbetalinger = listOf(delutbetaling1),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.DELUTBETALINGER_ALLEREDE_OPPRETTET,
            )
        }

        test("utbetales automatisk når det finnes et enkelt tilsagn med nok midler og det er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = tilsagnBeregning.copy(
                            output = tilsagnBeregning.output.copy(belop = 1000),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                val delutbetaling = queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldHaveSize(1).first()
                delutbetaling.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                delutbetaling.belop shouldBe 1000

                queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe Tiltaksadministrasjon
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                }

                queries.tilsagn.get(Tilsagn1.id).shouldNotBeNull().should {
                    it.belopBrukt shouldBe 1000
                }

                val records = queries.kafkaProducerRecord.getRecords(50)
                records.shouldHaveSize(1)
                Json.decodeFromString<OkonomiBestillingMelding>(records[0].value.decodeToString())
                    .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                    .payload.should {
                        it.belop shouldBe 1000
                        it.behandletAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.besluttetAv shouldBe Tiltaksadministrasjon.toOkonomiPart()
                        it.periode shouldBe Periode.forMonthOf(LocalDate.of(2025, 1, 1))
                        it.beskrivelse shouldBe """
                            Tiltakstype: Arbeidsforberedende trening (AFT)
                            Periode: 01.01.2025 - 31.01.2025
                            Tilsagnsnummer: A-2025/1-1
                        """.trimIndent()
                    }
            }
        }

        test("valideringsfeil hvis utbetaling forsøkes godkjennes flere ganger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeLeft(
                listOf(FieldError.of("Utbetaling er allerede godkjent")),
            )
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke er godkjent") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis ingen tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ).initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis flere tilsagn") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1, Tilsagn2.copy(periode = Tilsagn1.periode)),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
                setTilsagnStatus(Tilsagn2, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling hvis tilsagn ikke har nok penger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        beregning = tilsagnBeregning.copy(
                            output = TilsagnBeregningFri.Output(belop = 1),
                        ),
                    ),
                ),
                utbetalinger = listOf(utbetaling1Forhandsgodkjent),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.IKKE_NOK_PENGER,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("ingen automatisk utbetaling når prismodell er fri") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(Tilsagn1),
                utbetalinger = listOf(
                    utbetaling1Forhandsgodkjent.copy(
                        beregning = UtbetalingBeregningFri(
                            input = UtbetalingBeregningFri.Input(1),
                            output = UtbetalingBeregningFri.Output(1),
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()
            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.FEIL_PRISMODELL,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).shouldBeEmpty()
            }
        }

        test("Tilsagn gjøres ikke opp hvis det varer lengre enn utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            belop = 100,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val service = createUtbetalingService()

            service.godkjentAvArrangor(utbetaling1Id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1Id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe false
                }
            }
        }

        test("Tilsagn gjøres opp automatisk når siste dato i tilsagnsperioden er inkludert i utbetalingsperioden") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(
                    Tilsagn1.copy(
                        periode = Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 3, 1)),
                    ),
                ),
                utbetalinger = listOf(
                    utbetaling1.copy(
                        periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                        beregning = getForhandsgodkjentBeregning(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                            belop = 100,
                        ),
                    ),
                ),
            ) {
                setTilsagnStatus(Tilsagn1, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            val tilsagnService: TilsagnService = mockk(relaxed = true)
            val service = createUtbetalingService(tilsagnService = tilsagnService)

            service.godkjentAvArrangor(utbetaling1.id, kid = null).shouldBeRight(
                AutomatiskUtbetalingResult.GODKJENT,
            )

            database.run {
                queries.delutbetaling.getByUtbetalingId(utbetaling1.id).first().should {
                    it.status shouldBe DelutbetalingStatus.OVERFORT_TIL_UTBETALING
                    it.gjorOppTilsagn shouldBe true
                }
            }

            verify(exactly = 1) {
                tilsagnService.gjorOppAutomatisk(Tilsagn1.id, any())
            }
        }
    }
})

private fun QueryContext.setRoller(ansatt: NavAnsattDbo, roller: Set<NavAnsattRolle>) {
    queries.ansatt.setRoller(
        navIdent = ansatt.navIdent,
        roller = roller,
    )
}

private fun getForhandsgodkjentBeregning(periode: Periode, belop: Int) = UtbetalingBeregningForhandsgodkjent(
    input = UtbetalingBeregningForhandsgodkjent.Input(
        periode = periode,
        sats = 20205,
        stengt = setOf(),
        deltakelser = setOf(),
    ),
    output = UtbetalingBeregningForhandsgodkjent.Output(
        belop = belop,
        deltakelser = setOf(),
    ),
)
