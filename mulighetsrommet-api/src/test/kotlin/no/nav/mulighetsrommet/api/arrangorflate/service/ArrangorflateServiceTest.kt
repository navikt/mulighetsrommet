package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorflateTestUtils
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterType
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate

class ArrangorflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val utbetaling = ArrangorflateTestUtils.createTestUtbetalingForhandsgodkjent(deltaker.id)
    val friUtbetaling = ArrangorflateTestUtils.createTestUtbetalingFri()
    val kontoregisterOrganisasjon = mockk<KontoregisterOrganisasjonClient>(relaxed = true)

    val domain = ArrangorflateTestUtils.createTestDomain(
        deltaker = deltaker,
        tilsagn = tilsagn,
        utbetalinger = listOf(utbetaling, friUtbetaling),
    )
    val amtDeltakerClient = mockk<AmtDeltakerClient>()
    coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf<DeltakerPersonalia>().right()

    beforeEach {
        domain.initialize(database.db)
    }

    fun createService() = ArrangorflateService(
        database.db,
        amtDeltakerClient,
        kontoregisterOrganisasjon,
    )

    afterEach {
        database.truncateAll()
    }

    test("getUtbetaling should return utbetaling by ID") {
        val arrangorflateService = createService()

        val result = arrangorflateService.getUtbetaling(utbetaling.id)

        result.shouldNotBeNull()
        result.id shouldBe utbetaling.id

        result.beregning.shouldBeInstanceOf<UtbetalingBeregningFastSatsPerTiltaksplassPerManed>().should {
            it.output.pris shouldBe 10000.withValuta(Valuta.NOK)
        }
    }

    test("getTilsagn should return arrangorflateTilsagn by ID") {
        val arrangorflateService = createService()

        val result = arrangorflateService.getTilsagn(tilsagn.id)

        result.shouldNotBeNull()
        result.id shouldBe tilsagn.id
        result.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getArrangorflateTilsagnTilUtbetaling should return tilsagn for given gjennomforing and period") {
        val arrangorflateService = createService()

        val u = arrangorflateService.getUtbetaling(utbetaling.id)!!
        val result = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(
            u.copy(periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 8, 1))),
        )

        result shouldHaveSize 1
        result[0].id shouldBe tilsagn.id
    }

    test("getAdvarsler should return empty list when no advarsler exists") {
        val arrangorflateService = createService()

        val utbetalingDto = arrangorflateService.getUtbetaling(utbetaling.id)!!
        val result = arrangorflateService.getAdvarsler(utbetalingDto)
        result shouldHaveSize 0
    }

    test("mapUtbetalingToArrangorflateUtbetaling should have status KLAR_FOR_GODKJENNING") {
        val arrangorflateService = createService()
        val result = arrangorflateService.toArrangorflateUtbetaling(arrangorflateService.getUtbetaling(utbetaling.id)!!)
        result.id shouldBe utbetaling.id
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
    }

    test("mapUtbetalingToArrangorflateUtbetaling should convert a fri utbetaling") {
        val arrangorflateService = createService()
        var utbetaling = arrangorflateService.getUtbetaling(friUtbetaling.id)!!
        val result = arrangorflateService.toArrangorflateUtbetaling(utbetaling)

        result.id shouldBe friUtbetaling.id
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrangorflateBeregning> {
            it.pris shouldBe 5000.withValuta(Valuta.NOK)
            it.displayName shouldBe "Annen avtalt pris"
        }
    }

    test("toArrangorflateUtbetaling should map successfully with kanViseBeregning = true for recently approved utbetaling") {
        val arrangorflateService = createService()

        val date = LocalDate.now()
        val godkjentAvArrangorUtbetaling = arrangorflateService.getUtbetaling(utbetaling.id)!!.copy(
            innsending = Utbetaling.Innsending(date.atStartOfDay().minusDays(1)),
        )
        val result = arrangorflateService.toArrangorflateUtbetaling(godkjentAvArrangorUtbetaling, today = date)

        result.shouldNotBeNull()
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrangorflateBeregning> {
            it.deltakelser!!.rows shouldHaveSize 1
        }
        result.kanViseBeregning shouldBe true
    }

    test("toArrangorflateUtbetaling should map successfully with kanViseBeregning = false for 12 weeks old approved utbetaling") {
        val arrangorflateService = createService()

        val date = LocalDate.now()
        val godkjentAvArrangorUtbetaling = arrangorflateService.getUtbetaling(utbetaling.id)!!.copy(
            innsending = Utbetaling.Innsending(date.atStartOfDay().minusWeeks(12)),
        )
        val result = arrangorflateService.toArrangorflateUtbetaling(godkjentAvArrangorUtbetaling, today = date)

        result.shouldNotBeNull()
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrangorflateBeregning> {
            it.deltakelser!!.rows shouldHaveSize 1
            it.deltakelser.rows[0].cells["fnr"].shouldBeNull()
        }
        result.kanViseBeregning shouldBe false
    }

    test("getAllUtbetalingKompakt returnerer aktive utbetalinger for gitt arrangør") {
        val arrangorflateService = createService()

        val filter = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            type = ArrangorflateFilterType.AKTIVE,
        )
        val (totalCount, items) = arrangorflateService.getAllUtbetalingKompakt(filter)

        totalCount shouldBe 2
        items shouldHaveSize 2

        val forhandsgodkjent = items.first { it.id == utbetaling.id }
        forhandsgodkjent.pris shouldBe 10000.withValuta(Valuta.NOK)

        val fri = items.first { it.id == friUtbetaling.id }
        fri.pris shouldBe 5000.withValuta(Valuta.NOK)
    }

    test("getAllUtbetalingKompakt returnerer tom liste for historiske når alle utbetalinger er aktive") {
        val arrangorflateService = createService()

        val filter = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            type = ArrangorflateFilterType.HISTORISKE,
            pagination = Pagination.of(1, 50),
        )
        arrangorflateService.getAllUtbetalingKompakt(filter).totalCount shouldBe 0
    }

    test("getAllUtbetalingKompakt returnerer tom liste for ukjent arrangør") {
        val arrangorflateService = createService()

        val filter = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
            type = ArrangorflateFilterType.AKTIVE,
        )
        arrangorflateService.getAllUtbetalingKompakt(filter).totalCount shouldBe 0
    }

    test("getAllUtbetalingKompakt filtrerer på søketekst") {
        val arrangorflateService = createService()

        val filterMedTreff = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            sok = "Arbeidsfor",
        )
        arrangorflateService.getAllUtbetalingKompakt(filterMedTreff).totalCount shouldBe 2

        val filterUtenTreff = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            sok = "finnes-ikke-abc123",
            type = ArrangorflateFilterType.AKTIVE,
        )
        arrangorflateService.getAllUtbetalingKompakt(filterUtenTreff).totalCount shouldBe 0
    }

    test("getAllUtbetalingKompakt sorterer på beløp stigende") {
        val arrangorflateService = createService()

        val filter = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            type = ArrangorflateFilterType.AKTIVE,
            orderBy = ArrangorflateUtbetalingFilter.OrderBy.BELOP,
            direction = ArrangorflateFilterDirection.ASC,
        )
        val (_, items) = arrangorflateService.getAllUtbetalingKompakt(filter)

        items shouldHaveSize 2
        items[0].id shouldBe friUtbetaling.id
        items[0].pris shouldBe 5000.withValuta(Valuta.NOK)
        items[1].id shouldBe utbetaling.id
        items[1].pris shouldBe 10000.withValuta(Valuta.NOK)
    }

    test("getAllUtbetalingKompakt paginerer resultater riktig") {
        val arrangorflateService = createService()

        val filter = ArrangorflateUtbetalingFilter(
            arrangorer = setOf(ArrangorflateTestUtils.underenhet.organisasjonsnummer),
            type = ArrangorflateFilterType.AKTIVE,
            pagination = Pagination.of(1, 1),
        )
        val (totalCount, items) = arrangorflateService.getAllUtbetalingKompakt(filter)

        totalCount shouldBe 2
        items shouldHaveSize 1
    }
})
