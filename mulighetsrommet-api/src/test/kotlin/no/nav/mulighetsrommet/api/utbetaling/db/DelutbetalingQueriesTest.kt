package no.nav.mulighetsrommet.api.utbetaling.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DelutbetalingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
    )

    val tilsagn = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        periodeStart = LocalDate.of(2023, 1, 1),
        periodeSlutt = LocalDate.of(2023, 2, 1),
        kostnadssted = Gjovik.enhetsnummer,
        arrangorId = ArrangorFixtures.underenhet1.id,
        beregning = TilsagnBeregningFri(TilsagnBeregningFri.Input(123), TilsagnBeregningFri.Output(123)),
        endretAv = NavAnsattFixture.ansatt1.navIdent,
        endretTidspunkt = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        type = TilsagnType.TILSAGN,
    )
    val deltakelse1Id = UUID.randomUUID()
    val beregning = UtbetalingBeregningAft(
        input = UtbetalingBeregningAft.Input(
            sats = 20_205,
            periode = Periode.forMonthOf(LocalDate.of(2023, 1, 1)),
            deltakelser = setOf(
                DeltakelsePerioder(
                    deltakelseId = deltakelse1Id,
                    perioder = listOf(
                        DeltakelsePeriode(
                            start = LocalDate.of(2023, 1, 1),
                            slutt = LocalDate.of(2023, 1, 31),
                            deltakelsesprosent = 100.0,
                        ),
                    ),
                ),
            ),
        ),
        output = UtbetalingBeregningAft.Output(
            belop = 100_000,
            deltakelser = setOf(
                DeltakelseManedsverk(deltakelse1Id, 1.0),
            ),
        ),
    )

    test("opprett") {
        database.runAndRollback { session ->
            domain.setup(session)

            TilsagnQueries(session).upsert(tilsagn)
            val frist = LocalDate.of(2024, 10, 1).atStartOfDay()
            val utbetaling = UtbetalingDbo(
                id = UUID.randomUUID(),
                gjennomforingId = AFT1.id,
                fristForGodkjenning = frist,
                beregning = beregning,
                kontonummer = Kontonummer("11111111111"),
                kid = Kid("12345"),
                periode = beregning.input.periode,
            )
            UtbetalingQueries(session).upsert(utbetaling)

            val delutbetaling = DelutbetalingDbo(
                tilsagnId = tilsagn.id,
                utbetalingId = utbetaling.id,
                belop = 100,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
            DelutbetalingQueries(session).opprettDelutbetalinger(listOf(delutbetaling))

            val dto = DelutbetalingQueries(session).getByUtbetalingId(utbetaling.id)[0]
            dto shouldBe DelutbetalingDto.DelutbetalingTilGodkjenning(
                tilsagnId = tilsagn.id,
                utbetalingId = utbetaling.id,
                belop = 100,
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
            )
        }
    }
})
