package no.nav.mulighetsrommet.api.arrangorflate.service

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
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.tokenprovider.AccessType
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
    val personaliaService = mockk<PersonaliaService>()
    coEvery { personaliaService.getPersonalia(any(), any()) } returns emptyMap()

    beforeEach {
        domain.initialize(database.db)
    }

    fun createService() = ArrangorflateService(
        database.db,
        personaliaService,
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

    test("getArrangorflateTilsagnTilUtbetaling should return tilsagn for given gjennomforing and period") {
        val arrangorflateService = createService()

        val u = arrangorflateService.getUtbetaling(utbetaling.id)!!
        val result = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(
            u.copy(periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 8, 1))),
            AccessType.OBO.TokenX("token"),
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
        val result = arrangorflateService.toArrangorflateUtbetaling(
            arrangorflateService.getUtbetaling(utbetaling.id)!!,
            AccessType.OBO.TokenX("token"),
        )
        result.id shouldBe utbetaling.id
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
    }

    test("mapUtbetalingToArrangorflateUtbetaling should convert a fri utbetaling") {
        val arrangorflateService = createService()
        var utbetaling = arrangorflateService.getUtbetaling(friUtbetaling.id)!!
        val result = arrangorflateService.toArrangorflateUtbetaling(
            utbetaling,
            AccessType.OBO.TokenX("token"),
        )

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
        val result = arrangorflateService.toArrangorflateUtbetaling(
            godkjentAvArrangorUtbetaling,
            AccessType.OBO.TokenX("token"),
            today = date,
        )

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
        val result = arrangorflateService.toArrangorflateUtbetaling(
            godkjentAvArrangorUtbetaling,
            AccessType.OBO.TokenX("token"),
            today = date,
        )

        result.shouldNotBeNull()
        result.status shouldBe ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrangorflateBeregning> {
            it.deltakelser!!.rows shouldHaveSize 1
            it.deltakelser.rows[0].cells["fnr"].shouldBeNull()
        }
        result.kanViseBeregning shouldBe false
    }
})
